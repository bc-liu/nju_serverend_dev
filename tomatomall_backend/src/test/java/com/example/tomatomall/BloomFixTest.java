package com.example.tomatomall;

import com.example.tomatomall.utils.BloomFilterUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
public class BloomFixTest {
    @Autowired
    private BloomFilterUtil bloomFilterUtil;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    public void fixBloomFilter() {
        // 删除旧的
        redisTemplate.delete("bloom:product");
        System.out.println("已删除旧的 bloom:product");

        // 添加 product id = 1
        bloomFilterUtil.add("1");
        System.out.println("已添加 product id=1 到布隆过滤器");

        // 验证
        boolean contains1 = bloomFilterUtil.mightContain("1");
        boolean contains999 = bloomFilterUtil.mightContain("999");
        System.out.println("mightContain('1') = " + contains1);
        System.out.println("mightContain('999') = " + contains999);

        // 查看大小
        Long size = redisTemplate.opsForValue().size("bloom:product");
        System.out.println("bloom:product size = " + size + " bytes");
    }
}
