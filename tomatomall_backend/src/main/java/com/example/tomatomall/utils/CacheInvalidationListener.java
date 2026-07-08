package com.example.tomatomall.utils;

import com.github.benmanes.caffeine.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class CacheInvalidationListener {

    private static final Logger log = LoggerFactory.getLogger(CacheInvalidationListener.class);

    public static final String TOPIC_NAME = "tomatomall.cache.invalidation";

    @Autowired
    private Cache<String, String> productLocalCache;

    @KafkaListener(topics = TOPIC_NAME, groupId = "cache-invalidation-group")
    public void onMessage(@Payload String key,
                          @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
                          @Header(KafkaHeaders.OFFSET) long offset,
                          Acknowledgment acknowledgment) {
        if (key == null || key.isEmpty()) {
            log.warn("收到空的缓存失效key，跳过: partition={}, offset={}", partition, offset);
            acknowledgment.acknowledge();
            return;
        }

        try {
            String cacheKey = "product:" + key;
            productLocalCache.invalidate(cacheKey);
            
            log.info("L1缓存失效成功: key={}, cacheKey={}, partition={}, offset={}", 
                    key, cacheKey, partition, offset);
            
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("L1缓存失效失败: key={}, error={}", key, e.getMessage(), e);
            acknowledgment.acknowledge();
        }
    }
}
