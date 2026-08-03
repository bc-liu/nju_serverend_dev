package com.example.tomatomall.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Component
public class RedisCacheUtil implements DisposableBean {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private Cache<String, String> productLocalCache;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    // 商品缓存前缀
    private static final String PRODUCT_CACHE_PREFIX = "product:";

    // 互斥锁前缀
    private static final String LOCK_PREFIX = "lock:product:";

    // 缓存失效通知Topic名称
    private static final String CACHE_INVALIDATION_TOPIC = CacheInvalidationListener.TOPIC_NAME;

    // 默认缓存过期时间（分钟）
    private static final long DEFAULT_EXPIRE_MINUTES = 30;

    // 随机过期时间范围（分钟）
    private static final long RANDOM_EXPIRE_RANGE = 10;

    // 锁等待超时时间（毫秒）
    private static final long LOCK_WAIT_TIME_MS = 500;

    // 锁持有时间（毫秒）—— 修复：统一使用毫秒单位
    private static final long LOCK_LEASE_TIME_MS = 10 * 1000;

    // 延迟双删线程池
    private final ScheduledExecutorService delayedDeleteExecutor = Executors.newScheduledThreadPool(5);

    /**
     * 获取商品缓存（L1 Caffeine + L2 Redis 多级缓存）
     * 读取顺序：L1本地缓存 → L2 Redis缓存 → 数据库
     */
    public <T> T getProductCache(String key, Class<T> clazz, CacheLoader<T> cacheLoader,
            BloomFilterUtil bloomFilter) {
        String cacheKey = PRODUCT_CACHE_PREFIX + key;

        // 布隆过滤器防穿透：若 key 确定不存在，直接返回 null，避免穿透到 DB
        if (bloomFilter != null && !bloomFilter.mightContain(key)) {
            return null;
        }

        // 2. 查询L1本地缓存（Caffeine）
        String cachedValue = productLocalCache.getIfPresent(cacheKey);
        if (cachedValue != null) {
            if ("null".equals(cachedValue)) {
                return null;
            }
            try {
                return objectMapper.readValue(cachedValue, clazz);
            } catch (Exception e) {
                productLocalCache.invalidate(cacheKey);
            }
        }

        // 3. 查询L2 Redis缓存
        cachedValue = redisTemplate.opsForValue().get(cacheKey);
        if (cachedValue != null) {
            if ("null".equals(cachedValue)) {
                productLocalCache.put(cacheKey, "null");
                return null;
            }
            try {
                T result = objectMapper.readValue(cachedValue, clazz);
                productLocalCache.put(cacheKey, cachedValue);
                return result;
            } catch (Exception e) {
                redisTemplate.delete(cacheKey);
            }
        }

        // 4. L1和L2都未命中，尝试获取Redisson分布式锁
        String lockKey = LOCK_PREFIX + key;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 修复：waitTime和leaseTime统一使用毫秒单位
            boolean locked = lock.tryLock(LOCK_WAIT_TIME_MS, LOCK_LEASE_TIME_MS, TimeUnit.MILLISECONDS);

            if (!locked) {
                return null;
            }

            try {
                // 5. 双重检查：先查L1，再查L2
                cachedValue = productLocalCache.getIfPresent(cacheKey);
                if (cachedValue != null) {
                    if ("null".equals(cachedValue)) {
                        return null;
                    }
                    return objectMapper.readValue(cachedValue, clazz);
                }

                cachedValue = redisTemplate.opsForValue().get(cacheKey);
                if (cachedValue != null) {
                    if ("null".equals(cachedValue)) {
                        productLocalCache.put(cacheKey, "null");
                        return null;
                    }
                    T result = objectMapper.readValue(cachedValue, clazz);
                    productLocalCache.put(cacheKey, cachedValue);
                    return result;
                }

                // 6. 查询数据库
                T result = cacheLoader.load();

                // 7. 写入L2 Redis缓存和L1 Caffeine缓存
                if (result != null) {
                    String jsonValue = objectMapper.writeValueAsString(result);
                    long expireTime = getRandomExpireTime();
                    redisTemplate.opsForValue().set(cacheKey, jsonValue, expireTime, TimeUnit.MINUTES);
                    productLocalCache.put(cacheKey, jsonValue);

                    if (bloomFilter != null) {
                        bloomFilter.add(key);
                    }
                } else {
                    long expireTime = getRandomExpireTime() / 2;
                    redisTemplate.opsForValue().set(cacheKey, "null", expireTime, TimeUnit.MINUTES);
                    productLocalCache.put(cacheKey, "null");
                }

                return result;

            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取锁被中断: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("获取缓存失败: " + e.getMessage(), e);
        }
    }

    /**
     * 设置商品缓存（同时写入L1和L2）
     */
    public <T> void setProductCache(String key, T value) {
        String cacheKey = PRODUCT_CACHE_PREFIX + key;
        try {
            if (value != null) {
                String jsonValue = objectMapper.writeValueAsString(value);
                long expireTime = getRandomExpireTime();
                redisTemplate.opsForValue().set(cacheKey, jsonValue, expireTime, TimeUnit.MINUTES);
                productLocalCache.put(cacheKey, jsonValue);
            } else {
                long expireTime = getRandomExpireTime() / 2;
                redisTemplate.opsForValue().set(cacheKey, "null", expireTime, TimeUnit.MINUTES);
                productLocalCache.put(cacheKey, "null");
            }
        } catch (Exception e) {
            throw new RuntimeException("设置缓存失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除商品缓存（同时删除L1和L2，并通过Pub/Sub通知其他实例清除L1）
     */
    public void deleteProductCache(String key) {
        String cacheKey = PRODUCT_CACHE_PREFIX + key;
        productLocalCache.invalidate(cacheKey);
        redisTemplate.delete(cacheKey);
        // 发布缓存失效消息，通知其他实例清除L1本地缓存
        publishInvalidation(key);
    }

    /**
     * 延迟双删策略（同时删除L1和L2，并通过Pub/Sub通知其他实例）
     * 1. 先删除L1和L2缓存，发布失效通知
     * 2. 更新数据库
     * 3. 延迟一段时间后再次删除L1和L2缓存，再次发布失效通知
     */
    public void delayedDoubleDelete(String key, Runnable databaseUpdate) {
        String cacheKey = PRODUCT_CACHE_PREFIX + key;

        // 第一次删除L1和L2，并通知其他实例
        productLocalCache.invalidate(cacheKey);
        redisTemplate.delete(cacheKey);
        publishInvalidation(key);

        if (databaseUpdate != null) {
            databaseUpdate.run();
        }

        // 延迟后再次删除L1和L2，并通知其他实例
        delayedDeleteExecutor.schedule(() -> {
            productLocalCache.invalidate(cacheKey);
            redisTemplate.delete(cacheKey);
            publishInvalidation(key);
        }, 1, TimeUnit.SECONDS);
    }

    /**
     * 批量延迟双删
     */
    public void delayedDoubleDelete(Iterable<String> keys, Runnable databaseUpdate) {
        for (String key : keys) {
            deleteProductCache(key);
        }

        if (databaseUpdate != null) {
            databaseUpdate.run();
        }

        delayedDeleteExecutor.schedule(() -> {
            for (String key : keys) {
                deleteProductCache(key);
            }
        }, 1, TimeUnit.SECONDS);
    }

    /**
     * 批量删除商品缓存
     */
    public void deleteProductCache(Iterable<String> keys) {
        for (String key : keys) {
            deleteProductCache(key);
        }
    }

    /**
     * 发布缓存失效消息到Redis Pub/Sub，通知其他实例清除L1本地缓存
     */
    private void publishInvalidation(String key) {
        try {
            kafkaTemplate.send(CACHE_INVALIDATION_TOPIC, key);
        } catch (Exception e) {
            // Kafka发送失败不影响主流程，仅记录日志
        }
    }

    /**
     * 获取带随机因子的过期时间
     */
    private long getRandomExpireTime() {
        long randomOffset = ThreadLocalRandom.current().nextInt((int) RANDOM_EXPIRE_RANGE);
        return DEFAULT_EXPIRE_MINUTES + randomOffset;
    }

    /**
     * 修复：Spring容器销毁时优雅关闭线程池，防止资源泄漏
     */
    @Override
    public void destroy() {
        delayedDeleteExecutor.shutdown();
        try {
            if (!delayedDeleteExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                delayedDeleteExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            delayedDeleteExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 缓存加载器接口
     */
    @FunctionalInterface
    public interface CacheLoader<T> {
        T load();
    }
}
