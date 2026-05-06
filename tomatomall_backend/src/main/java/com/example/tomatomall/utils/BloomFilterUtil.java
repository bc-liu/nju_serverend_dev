package com.example.tomatomall.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.BitSet;

@Component
public class BloomFilterUtil {

    @Autowired
    private StringRedisTemplate redisTemplate;

    // 布隆过滤器名称
    private static final String BLOOM_FILTER_KEY = "bloom:product";
    
    // 布隆过滤器大小（位数组大小）
    private static final int BIT_SIZE = 2 << 28; // 约5.36亿位
    
    // 哈希函数数量
    private static final int HASH_COUNT = 8;

    /**
     * 添加元素到布隆过滤器
     */
    public void add(String value) {
        int[] offsets = getHashOffsets(value);
        for (int offset : offsets) {
            redisTemplate.opsForValue().setBit(BLOOM_FILTER_KEY, offset, true);
        }
    }

    /**
     * 批量添加元素到布隆过滤器
     */
    public void addAll(Iterable<String> values) {
        for (String value : values) {
            add(value);
        }
    }

    /**
     * 检查元素是否可能存在
     */
    public boolean mightContain(String value) {
        int[] offsets = getHashOffsets(value);
        for (int offset : offsets) {
            if (!redisTemplate.opsForValue().getBit(BLOOM_FILTER_KEY, offset)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 初始化布隆过滤器（添加所有存在的商品ID）
     */
    public void initBloomFilter(Iterable<Integer> productIds) {
        for (Integer productId : productIds) {
            add(String.valueOf(productId));
        }
    }

    /**
     * 获取哈希偏移量
     */
    private int[] getHashOffsets(String value) {
        int[] offsets = new int[HASH_COUNT];
        
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] digest = md5.digest(value.getBytes(StandardCharsets.UTF_8));
            
            // 使用不同的哈希种子生成多个哈希值
            for (int i = 0; i < HASH_COUNT; i++) {
                // 使用不同的种子生成哈希值
                long hash = murmurHash(value + i);
                offsets[i] = Math.abs((int) (hash % BIT_SIZE));
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
        
        return offsets;
    }

    /**
     * MurmurHash算法实现
     */
    private long murmurHash(String key) {
        byte[] data = key.getBytes(StandardCharsets.UTF_8);
        final int m = 0x5bd1e995;
        final int r = 24;
        int len = data.length;
        int h = 0x9747b28c ^ len;

        int i = 0;
        while (len >= 4) {
            int k = data[i] & 0xFF;
            k |= (data[i + 1] & 0xFF) << 8;
            k |= (data[i + 2] & 0xFF) << 16;
            k |= (data[i + 3] & 0xFF) << 24;

            k *= m;
            k ^= k >>> r;
            k *= m;

            h *= m;
            h ^= k;

            i += 4;
            len -= 4;
        }

        switch (len) {
            case 3:
                h ^= (data[i + 2] & 0xFF) << 16;
            case 2:
                h ^= (data[i + 1] & 0xFF) << 8;
            case 1:
                h ^= data[i] & 0xFF;
                h *= m;
        }

        h ^= h >>> 13;
        h *= m;
        h ^= h >>> 15;

        return h & 0xFFFFFFFFL;
    }

    /**
     * 清空布隆过滤器
     */
    public void clear() {
        redisTemplate.delete(BLOOM_FILTER_KEY);
    }

    /**
     * 获取布隆过滤器位图大小
     */
    public long getBitCount() {
        Long size = redisTemplate.opsForValue().size(BLOOM_FILTER_KEY);
        return size != null ? size : 0;
    }
}