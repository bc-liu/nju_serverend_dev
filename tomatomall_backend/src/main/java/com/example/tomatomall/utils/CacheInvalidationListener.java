package com.example.tomatomall.utils;

import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class CacheInvalidationListener {

    public static final String CHANNEL_NAME = "cache:invalidation:product";

    @Autowired
    private Cache<String, String> productLocalCache;

    @Autowired
    private RedisMessageListenerContainer redisMessageListenerContainer;

    @PostConstruct
    public void init() {
        MessageListenerAdapter adapter = new MessageListenerAdapter(this, "onMessage");
        redisMessageListenerContainer.addMessageListener(adapter, new ChannelTopic(CHANNEL_NAME));
    }

    /**
     * 接收Redis Pub/Sub缓存失效消息，清除本地L1 Caffeine缓存
     */
    public void onMessage(Message message, byte[] pattern) {
        String key = new String(message.getBody(), StandardCharsets.UTF_8);
        String cacheKey = "product:" + key;
        productLocalCache.invalidate(cacheKey);
    }
}
