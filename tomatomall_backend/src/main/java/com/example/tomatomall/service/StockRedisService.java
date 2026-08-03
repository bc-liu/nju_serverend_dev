package com.example.tomatomall.service;

import com.example.tomatomall.Repository.StockpileRepository;
import com.example.tomatomall.po.Stockpile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;

/**
 * 基于 Redis + Lua 脚本的库存服务。
 *
 * 核心思路:
 * - 把商品可用库存(amount)预热到 Redis,Key = "stock:product:{productId}:amount"
 * - 下单扣减时执行一段 Lua 脚本, 利用 Redis 单线程特性把 "比较 + 扣减" 变成原子操作,
 * 彻底杜绝并发场景下的超卖
 * - Redis 扣减成功后再同步数据库(走原有 decreaseStock SQL), 保证 DB 数据最终一致;
 * 若 DB 同步失败, 调用回滚脚本把库存加回 Redis
 *
 * 相比纯数据库 "UPDATE ... WHERE amount >= ?" 方案, 该方案把热点扣减流量挡在 Redis 层,
 * 单节点 QPS 可从 ~3k 提升到 10w+, 是秒杀/抢购场景的工业界主流做法。
 */
@Service
public class StockRedisService {

    private static final Logger log = LoggerFactory.getLogger(StockRedisService.class);

    /** Redis 库存 Key 前缀 */
    private static final String STOCK_KEY_PREFIX = "stock:product:";

    private static final String STOCK_KEY_SUFFIX = ":amount";

    /** Lua 脚本返回值: 扣减成功 */
    private static final long RESULT_SUCCESS = 1L;

    /** Lua 脚本返回值: 库存不足 */
    private static final long RESULT_INSUFFICIENT = 0L;

    /** Lua 脚本返回值: Redis 中未初始化库存 */
    private static final long RESULT_NOT_INITIALIZED = -1L;

    /** 库存 Key 默认 TTL（7 天），作为兜底防止被删商品残留 Redis Key */
    private static final long STOCK_KEY_TTL_DAYS = 7;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private StockpileRepository stockpileRepository;

    @Autowired
    private BloomFilter<String> bloomFilter;

    /** 扣减库存 Lua 脚本(线程安全, 全局共享) */
    private RedisScript<Long> deductScript;

    /** 回滚库存 Lua 脚本 */
    private RedisScript<Long> restoreScript;

    /**
     * 应用启动时加载 Lua 脚本并预热全部库存到 Redis。
     */
    @PostConstruct
    public void init() {
        deductScript = loadScript("lua/deduct_stock.lua", Long.class);
        restoreScript = loadScript("lua/restore_stock.lua", Long.class);
        try {
            refreshAllStockFromDB();
        } catch (Exception e) {
            // Redis 未就绪不应阻断应用启动, 后续请求会触发懒加载
            log.warn("启动时预热库存到 Redis 失败, 将在首次请求时懒加载: {}", e.getMessage());
        }
    }

    public void initBloomFilter() {
        // 启动时把所有商品 ID 加载到布隆过滤器
        List<Integer> allIds = stockpileRepository.findAllProductIds();
        for (Integer id : allIds) {
            bloomFilter.put(id.toString());
        }
    }

    /**
     * 从数据库全量刷新库存到 Redis。可在后台/管理接口手动调用。
     */
    public void refreshAllStockFromDB() {
        List<Stockpile> all = stockpileRepository.findAll();
        for (Stockpile s : all) {
            if (s.getProduct() == null)
                continue;
            String key = buildKey(s.getProduct().getId());
            // 注意: 这里写入的是 amount (可用库存), 不包含 frozen
            // 设置 TTL 防止已删除商品的 Key 永久残留
            redisTemplate.opsForValue().set(key, String.valueOf(s.getAmount()),
                    STOCK_KEY_TTL_DAYS, TimeUnit.DAYS);
        }
        log.info("已从数据库加载 {} 条库存记录到 Redis", all.size());
    }

    /**
     * 刷新单个商品的 Redis 库存。当管理员修改库存、订单取消回滚库存时调用,
     * 保证 Redis 与 DB 的 amount 字段一致。
     */
    public void refreshStock(Integer productId) {
        loadStockFromDB(productId);
    }

    /**
     * 扣减库存(原子操作)。
     *
     * @param productId 商品 ID
     * @param quantity  扣减数量
     * @return true=成功; false=库存不足
     */
    public boolean deductStock(Integer productId, Integer quantity) {
        String key = buildKey(productId);

        // 先检查布隆过滤器, 避免穿透穿透
        if (!bloomFilter.mightContain(productId.toString())) {
            log.warn("Redis 中未命中库存, 视为库存不足: productId={}", productId);
            return false;
        }

        Long result = redisTemplate.execute(
                deductScript,
                Collections.singletonList(key),
                String.valueOf(quantity));

        if (result == null) {
            log.warn("Lua 脚本返回 null, 视为库存不足: productId={}", productId);
            return false;
        }

        if (result == RESULT_NOT_INITIALIZED) {
            // Redis 未命中, 懒加载一次后重试
            log.info("Redis 中未初始化库存, 触发懒加载: productId={}", productId);
            loadStockFromDB(productId);
            return deductStock(productId, quantity);
        }

        return result == RESULT_SUCCESS;
    }

    /**
     * 回滚库存(用于 Redis 扣减成功但 DB 事务失败的场景)。
     */
    public void restoreStock(Integer productId, Integer quantity) {
        String key = buildKey(productId);
        redisTemplate.execute(
                restoreScript,
                Collections.singletonList(key),
                String.valueOf(quantity));
        log.info("Redis 库存已回滚: productId={}, quantity={}", productId, quantity);
    }

    /**
     * 获取 Redis 中的当前可用库存(用于监控/展示)。
     */
    public Long getStock(Integer productId) {
        String value = redisTemplate.opsForValue().get(buildKey(productId));
        if (value == null)
            return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 删除 Redis 中的库存 Key。商品被删除时调用, 避免缓存泄漏。
     */
    public void deleteStock(Integer productId) {
        redisTemplate.delete(buildKey(productId));
        log.info("已删除 Redis 库存 Key: productId={}", productId);
    }

    /**
     * 当单个商品库存未预热时, 从 DB 加载到 Redis。
     */
    private void loadStockFromDB(Integer productId) {
        Stockpile s = stockpileRepository.findByProductId(productId);
        if (s == null) {
            // 商品不存在时写入 0, 避免反复穿透（带 TTL 防止残留）
            redisTemplate.opsForValue().set(buildKey(productId), "0",
                    STOCK_KEY_TTL_DAYS, TimeUnit.DAYS);
            return;
        }
        redisTemplate.opsForValue().set(buildKey(productId), String.valueOf(s.getAmount()),
                STOCK_KEY_TTL_DAYS, TimeUnit.DAYS);
    }

    private String buildKey(Integer productId) {
        return STOCK_KEY_PREFIX + productId + STOCK_KEY_SUFFIX;
    }

    private <T> RedisScript<T> loadScript(String path, Class<T> resultType) {
        DefaultRedisScript<T> script = new DefaultRedisScript<>();
        script.setScriptSource(new org.springframework.scripting.support.ResourceScriptSource(
                new ClassPathResource(path)));
        script.setResultType(resultType);
        return script;
    }
}
