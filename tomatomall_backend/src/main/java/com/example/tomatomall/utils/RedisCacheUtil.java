package com.example.tomatomall.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class RedisCacheUtil {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedissonClient redissonClient;

    // 商品缓存前缀
    private static final String PRODUCT_CACHE_PREFIX = "product:";

    // 互斥锁前缀
    private static final String LOCK_PREFIX = "lock:product:";

    // 默认缓存过期时间（分钟）
    private static final long DEFAULT_EXPIRE_MINUTES = 30;

    // 随机过期时间范围（分钟）
    private static final long RANDOM_EXPIRE_RANGE = 10;

    // 锁等待超时时间（毫秒）
    private static final long LOCK_WAIT_TIME_MS = 500;

    // 锁持有时间（秒）
    private static final long LOCK_LEASE_TIME_SECONDS = 10;

    // 延迟双删线程池
    private final ScheduledExecutorService delayedDeleteExecutor = Executors.newScheduledThreadPool(5);

    /**
     * 获取商品缓存（包含防击穿和防穿透逻辑）
     * 使用Redisson RLock实现分布式锁，支持超时与快速失败
     */
    public <T> T getProductCache(String key, Class<T> clazz, CacheLoader<T> cacheLoader,
            BloomFilterUtil bloomFilter) {
        String cacheKey = PRODUCT_CACHE_PREFIX + key;

        // 1. 先检查布隆过滤器（防穿透）
        if (bloomFilter != null && !bloomFilter.mightContain(key)) {
            return null;
        }

        // 2. 查询缓存
        String cachedValue = redisTemplate.opsForValue().get(cacheKey);
        if (cachedValue != null) {
            if ("null".equals(cachedValue)) {
                return null;
            }
            try {
                return objectMapper.readValue(cachedValue, clazz);
            } catch (Exception e) {
                redisTemplate.delete(cacheKey);
            }
        }

        // 3. 缓存未命中，尝试获取Redisson分布式锁
        String lockKey = LOCK_PREFIX + key;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取锁，设置等待超时和锁持有时间
            boolean locked = lock.tryLock(LOCK_WAIT_TIME_MS, LOCK_LEASE_TIME_SECONDS, TimeUnit.MILLISECONDS);

            if (!locked) {
                // 获取锁超时，快速失败，返回null或兜底数据
                return null;
            }

            try {
                // 4. 双重检查缓存（防止其他线程已经写入缓存）
                cachedValue = redisTemplate.opsForValue().get(cacheKey);
                if (cachedValue != null) {
                    if ("null".equals(cachedValue)) {
                        return null;
                    }
                    return objectMapper.readValue(cachedValue, clazz);
                }

                // 5. 查询数据库
                T result = cacheLoader.load();

                // 6. 写入缓存
                if (result != null) {
                    String jsonValue = objectMapper.writeValueAsString(result);
                    long expireTime = getRandomExpireTime();
                    redisTemplate.opsForValue().set(cacheKey, jsonValue, expireTime, TimeUnit.MINUTES);

                    if (bloomFilter != null) {
                        bloomFilter.add(key);
                    }
                } else {
                    long expireTime = getRandomExpireTime() / 2;
                    redisTemplate.opsForValue().set(cacheKey, "null", expireTime, TimeUnit.MINUTES);
                }

                return result;

            } finally {
                // Redisson自动管理锁释放，确保锁一定会被释放
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
     * 设置商品缓存
     */
    public <T> void setProductCache(String key, T value) {
        String cacheKey = PRODUCT_CACHE_PREFIX + key;
        try {
            if (value != null) {
                String jsonValue = objectMapper.writeValueAsString(value);
                long expireTime = getRandomExpireTime();
                redisTemplate.opsForValue().set(cacheKey, jsonValue, expireTime, TimeUnit.MINUTES);
            } else {
                long expireTime = getRandomExpireTime() / 2;
                redisTemplate.opsForValue().set(cacheKey, "null", expireTime, TimeUnit.MINUTES);
            }
        } catch (Exception e) {
            throw new RuntimeException("设置缓存失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除商品缓存
     */
    public void deleteProductCache(String key) {
        String cacheKey = PRODUCT_CACHE_PREFIX + key;
        redisTemplate.delete(cacheKey);
    }

    /**
     * 延迟双删策略
     * 1. 先删除缓存
     * 2. 更新数据库
     * 3. 延迟一段时间后再次删除缓存
     */
    public void delayedDoubleDelete(String key, Runnable databaseUpdate) {
        String cacheKey = PRODUCT_CACHE_PREFIX + key;

        redisTemplate.delete(cacheKey);

        if (databaseUpdate != null) {
            databaseUpdate.run();
        }

        delayedDeleteExecutor.schedule(() -> {
            redisTemplate.delete(cacheKey);
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
     * 获取带随机因子的过期时间
     */
    private long getRandomExpireTime() {
        Random random = new Random();
        long randomOffset = random.nextInt((int) RANDOM_EXPIRE_RANGE);
        return DEFAULT_EXPIRE_MINUTES + randomOffset;
    }

    /**
     * 缓存加载器接口
     */
    @FunctionalInterface
    public interface CacheLoader<T> {
        T load();
    }
}
