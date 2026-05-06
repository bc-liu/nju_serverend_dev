package com.example.tomatomall.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class RedisCacheUtil {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // 商品缓存前缀
    private static final String PRODUCT_CACHE_PREFIX = "product:";
    
    // 互斥锁前缀
    private static final String LOCK_PREFIX = "lock:product:";
    
    // 默认缓存过期时间（分钟）
    private static final long DEFAULT_EXPIRE_MINUTES = 30;
    
    // 随机过期时间范围（分钟）
    private static final long RANDOM_EXPIRE_RANGE = 10;
    
    // 互斥锁过期时间（秒）
    private static final long LOCK_EXPIRE_SECONDS = 10;
    
    // 本地锁，防止同一JVM内的重复查询
    private final ReentrantLock localLock = new ReentrantLock();

    /**
     * 获取商品缓存（包含防击穿和防穿透逻辑）
     */
    public <T> T getProductCache(String key, Class<T> clazz, CacheLoader<T> cacheLoader, 
                                BloomFilterUtil bloomFilter) {
        String cacheKey = PRODUCT_CACHE_PREFIX + key;
        
        // 1. 先检查布隆过滤器（防穿透）
        if (bloomFilter != null && !bloomFilter.mightContain(key)) {
            return null; // 商品不存在，直接返回null
        }
        
        // 2. 查询缓存
        String cachedValue = redisTemplate.opsForValue().get(cacheKey);
        if (cachedValue != null) {
            if ("null".equals(cachedValue)) {
                return null; // 缓存空值
            }
            try {
                return objectMapper.readValue(cachedValue, clazz);
            } catch (Exception e) {
                // 缓存数据格式错误，删除缓存并重新查询
                redisTemplate.delete(cacheKey);
            }
        }
        
        // 3. 缓存未命中，尝试获取分布式锁
        String lockKey = LOCK_PREFIX + key;
        boolean locked = false;
        
        try {
            // 先尝试获取本地锁，防止同一JVM内的重复查询
            localLock.lock();
            
            // 再尝试获取Redis分布式锁
            locked = tryLock(lockKey);
            
            if (!locked) {
                // 获取锁失败，等待并重试
                Thread.sleep(100);
                return getProductCache(key, clazz, cacheLoader, bloomFilter);
            }
            
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
                
                // 更新布隆过滤器
                if (bloomFilter != null) {
                    bloomFilter.add(key);
                }
            } else {
                // 缓存空值，防止缓存穿透
                long expireTime = getRandomExpireTime() / 2; // 空值缓存时间较短
                redisTemplate.opsForValue().set(cacheKey, "null", expireTime, TimeUnit.MINUTES);
            }
            
            return result;
            
        } catch (Exception e) {
            throw new RuntimeException("获取缓存失败: " + e.getMessage(), e);
        } finally {
            // 释放锁
            if (locked) {
                releaseLock(lockKey);
            }
            localLock.unlock();
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
                // 缓存空值
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
     * 尝试获取分布式锁
     */
    private boolean tryLock(String lockKey) {
        Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", 
                LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 释放分布式锁
     */
    private void releaseLock(String lockKey) {
        redisTemplate.delete(lockKey);
    }

    /**
     * 缓存加载器接口
     */
    @FunctionalInterface
    public interface CacheLoader<T> {
        T load();
    }
}