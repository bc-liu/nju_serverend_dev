package com.example.tomatomall.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Slf4j
public class ApiCallStatisticsAspect {

    @Autowired
    private StringRedisTemplate redisTemplate;

    // 定义切点，拦截所有Controller中的方法
    @Pointcut("execution(* com.example.tomatomall.controller.*.*(..))")
    public void apiPointcut() {
    }

    @Around("apiPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // 执行目标方法
        Object result = joinPoint.proceed();

        stopWatch.stop();
        long executionTime = stopWatch.getTotalTimeMillis();

        // 获取类名和方法名
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String apiKey = "api:statistics:" + className + ":" + methodName;

        // 统计调用次数
        redisTemplate.opsForValue().increment(apiKey + ":count");

        // 统计总响应时间
        redisTemplate.opsForValue().increment(apiKey + ":totalTime", executionTime);

        // 记录最近一次调用时间
        redisTemplate.opsForValue().set(apiKey + ":lastCall", String.valueOf(System.currentTimeMillis()));

        // 设置过期时间为7天
        redisTemplate.expire(apiKey + ":count", 7, TimeUnit.DAYS);
        redisTemplate.expire(apiKey + ":totalTime", 7, TimeUnit.DAYS);
        redisTemplate.expire(apiKey + ":lastCall", 7, TimeUnit.DAYS);

        log.info("API调用统计 - {}:{} 执行时间: {}ms", className, methodName, executionTime);

        return result;
    }
}
