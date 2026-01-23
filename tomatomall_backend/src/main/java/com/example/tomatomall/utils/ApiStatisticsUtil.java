package com.example.tomatomall.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class ApiStatisticsUtil {

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 获取所有接口的调用统计信息
     */
    public Map<String, Map<String, Object>> getAllApiStatistics() {
        Map<String, Map<String, Object>> result = new HashMap<>();

        // 获取所有API统计键
        Set<String> keys = redisTemplate.keys("api:statistics:*:count");

        if (keys != null) {
            for (String key : keys) {
                // 提取API标识
                String apiIdentifier = key.replace("api:statistics:", "").replace(":count", "");
                String[] parts = apiIdentifier.split(":");
                if (parts.length >= 2) {
                    String className = parts[0];
                    String methodName = parts[1];

                    // 获取统计数据
                    String countKey = "api:statistics:" + apiIdentifier + ":count";
                    String totalTimeKey = "api:statistics:" + apiIdentifier + ":totalTime";
                    String lastCallKey = "api:statistics:" + apiIdentifier + ":lastCall";

                    String count = redisTemplate.opsForValue().get(countKey);
                    String totalTime = redisTemplate.opsForValue().get(totalTimeKey);
                    String lastCall = redisTemplate.opsForValue().get(lastCallKey);

                    // 计算平均响应时间
                    double avgTime = 0;
                    if (count != null && totalTime != null && Integer.parseInt(count) > 0) {
                        avgTime = Double.parseDouble(totalTime) / Integer.parseInt(count);
                    }

                    // 构建统计信息
                    Map<String, Object> stats = new HashMap<>();
                    stats.put("count", count != null ? Integer.parseInt(count) : 0);
                    stats.put("totalTime", totalTime != null ? Long.parseLong(totalTime) : 0);
                    stats.put("avgTime", avgTime);
                    stats.put("lastCall", lastCall != null ? Long.parseLong(lastCall) : 0);

                    result.put(className + ":" + methodName, stats);
                }
            }
        }

        return result;
    }

    /**
     * 获取指定接口的调用统计信息
     */
    public Map<String, Object> getApiStatistics(String className, String methodName) {
        String apiIdentifier = className + ":" + methodName;

        String countKey = "api:statistics:" + apiIdentifier + ":count";
        String totalTimeKey = "api:statistics:" + apiIdentifier + ":totalTime";
        String lastCallKey = "api:statistics:" + apiIdentifier + ":lastCall";

        String count = redisTemplate.opsForValue().get(countKey);
        String totalTime = redisTemplate.opsForValue().get(totalTimeKey);
        String lastCall = redisTemplate.opsForValue().get(lastCallKey);

        // 计算平均响应时间
        double avgTime = 0;
        if (count != null && totalTime != null && Integer.parseInt(count) > 0) {
            avgTime = Double.parseDouble(totalTime) / Integer.parseInt(count);
        }

        // 构建统计信息
        Map<String, Object> stats = new HashMap<>();
        stats.put("count", count != null ? Integer.parseInt(count) : 0);
        stats.put("totalTime", totalTime != null ? Long.parseLong(totalTime) : 0);
        stats.put("avgTime", avgTime);
        stats.put("lastCall", lastCall != null ? Long.parseLong(lastCall) : 0);

        return stats;
    }
}
