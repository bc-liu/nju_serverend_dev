package com.example.tomatomall.utils;

import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CacheInvalidationListener {

    public static final String TOPIC_NAME = "tomatomall.cache.invalidation";

    @Autowired
    private Cache<String, String> productLocalCache;

    @KafkaListener(topics = TOPIC_NAME, groupId = "cache-invalidation-group")
    public void onMessage(String key) {
        if (key == null || key.isEmpty()) {
            return;
        }
        String cacheKey = "product:" + key;
        productLocalCache.invalidate(cacheKey);
    }
}
