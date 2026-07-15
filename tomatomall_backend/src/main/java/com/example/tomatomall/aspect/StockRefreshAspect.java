package com.example.tomatomall.aspect;

import com.example.tomatomall.annotation.AutoRefreshStock;
import com.example.tomatomall.service.StockRedisService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 自动同步 Redis 库存的 AOP 切面。
 *
 * 拦截所有标注了 {@link AutoRefreshStock} 的方法, 在方法成功返回后:
 *   - 若 delete = false: 调用 {@link StockRedisService#refreshStock} 从 DB 重新加载
 *   - 若 delete = true : 调用 {@link StockRedisService#deleteStock} 删除 Redis Key
 *
 * 通过 SpEL 表达式从方法入参/返回值中解析 productId, 避免硬编码参数位置。
 * 方法抛异常时切面不触发, Redis 保持原值 (DB 事务已回滚, 无需同步)。
 */
@Aspect
@Component
public class StockRefreshAspect {

    private static final Logger log = LoggerFactory.getLogger(StockRefreshAspect.class);

    private final ExpressionParser parser = new SpelExpressionParser();

    private final ParameterNameDiscoverer paramNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Autowired
    private StockRedisService stockRedisService;

    @AfterReturning("@annotation(autoRefresh)")
    public void afterReturning(JoinPoint joinPoint, AutoRefreshStock autoRefresh) {
        Integer productId;
        try {
            productId = parseProductId(autoRefresh.productId(), joinPoint);
        } catch (Exception e) {
            log.warn("解析 @AutoRefreshStock productId 表达式失败: expr={}, error={}",
                    autoRefresh.productId(), e.getMessage());
            return;
        }

        if (productId == null) {
            log.warn("@AutoRefreshStock productId 解析为 null, 跳过同步: expr={}", autoRefresh.productId());
            return;
        }

        try {
            if (autoRefresh.delete()) {
                stockRedisService.deleteStock(productId);
            } else {
                stockRedisService.refreshStock(productId);
            }
            log.debug("AOP 自动同步 Redis 库存: productId={}, delete={}", productId, autoRefresh.delete());
        } catch (Exception e) {
            // Redis 同步失败不应影响主业务, 仅记录日志
            log.error("AOP 自动同步 Redis 库存失败: productId={}, error={}", productId, e.getMessage(), e);
        }
    }

    /**
     * 用 SpEL 表达式从方法入参中解析 productId。
     */
    private Integer parseProductId(String spelExpr, JoinPoint joinPoint) {
        Method method = resolveMethod(joinPoint);
        EvaluationContext context = new MethodBasedEvaluationContext(
                null, method, joinPoint.getArgs(), paramNameDiscoverer);

        Expression expression = parser.parseExpression(spelExpr);
        Object value = expression.getValue(context);

        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.valueOf(value.toString());
    }

    private Method resolveMethod(JoinPoint joinPoint) {
        // 用签名获取 Method 对象, 保证 SpEL 能按参数名访问
        try {
            String methodName = joinPoint.getSignature().getName();
            Class<?> targetClass = joinPoint.getTarget().getClass();
            Class<?>[] paramTypes = ((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature())
                    .getParameterTypes();
            return targetClass.getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("无法解析目标方法: " + joinPoint.getSignature(), e);
        }
    }
}
