package com.example.tomatomall.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注该方法修改了数据库库存, 需要由 AOP 切面自动同步 Redis。
 *
 * 用法示例:
 * <pre>
 * &#64;AutoRefreshStock(productId = "#id")
 * public void adjustStockPile(Integer id, Integer amount) { ... }
 *
 * &#64;AutoRefreshStock(productId = "#stockpileVO.productId")
 * public void addStockPile(ProductVO.StockpileVO stockpileVO) { ... }
 *
 * &#64;AutoRefreshStock(productId = "#id", delete = true)
 * public void deleteProduct(Integer id) { ... }
 * </pre>
 *
 * SpEL 上下文变量:
 *   - #参数名  : 方法入参 (依赖 -parameters 编译选项, Spring Boot 默认开启)
 *   - #result  : 方法返回值
 *   - #p0/#a0  : 按位置索引的参数
 *
 * delete = true 时删除 Redis Key (用于商品删除场景), 否则从 DB 重新加载库存到 Redis。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoRefreshStock {

    /**
     * 指向 productId 的 SpEL 表达式。
     */
    String productId();

    /**
     * 是否为删除场景。true = 删除 Redis Key; false = 从 DB 刷新到 Redis。
     */
    boolean delete() default false;
}
