package com.example.tomatomall;

import com.example.tomatomall.Repository.StockpileRepository;
import com.example.tomatomall.po.Product;
import com.example.tomatomall.po.Stockpile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 库存扣减原子性测试 (DataJpaTest)
 * 验证 decreaseStock JPQL 查询在并发安全中的正确性
 */
@DataJpaTest
@ActiveProfiles("test")
class StockpileRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private StockpileRepository stockpileRepository;

    private Stockpile stockpile;

    @BeforeEach
    void setUp() {
        // Stockpile.product 是 nullable=false, 必须先建 Product
        Product product = new Product();
        product.setTitle("测试商品");
        product.setPrice(new BigDecimal("99.00"));
        product.setRate(4.5);
        product = entityManager.persistAndFlush(product);

        // 创建库存记录：amount=10, frozen=0
        stockpile = new Stockpile();
        stockpile.setAmount(10);
        stockpile.setFrozen(0);
        stockpile.setProduct(product);
        stockpile = entityManager.persistAndFlush(stockpile);
    }

    @Test
    @DisplayName("库存充足时扣减成功，返回更新行数=1")
    void decreaseStock_WhenStockSufficient_ShouldReturnOne() {
        Integer stockpileId = stockpile.getId();

        int updatedRows = stockpileRepository.decreaseStock(stockpileId, 3);

        assertEquals(1, updatedRows, "应成功更新 1 行");

        // 刷新并验证 amount 和 frozen
        entityManager.clear();
        Stockpile updated = stockpileRepository.findById(stockpileId).orElseThrow();
        assertEquals(7, updated.getAmount(), "amount 应从 10 减到 7");
        assertEquals(3, updated.getFrozen(), "frozen 应从 0 增到 3");
    }

    @Test
    @DisplayName("库存不足时扣减失败，返回更新行数=0")
    void decreaseStock_WhenStockInsufficient_ShouldReturnZero() {
        Integer stockpileId = stockpile.getId();

        // 尝试扣减 100（库存只有 10）
        int updatedRows = stockpileRepository.decreaseStock(stockpileId, 100);

        assertEquals(0, updatedRows, "库存不足应返回 0（未更新任何行）");

        // 确认库存未被修改
        entityManager.clear();
        Stockpile unchanged = stockpileRepository.findById(stockpileId).orElseThrow();
        assertEquals(10, unchanged.getAmount(), "amount 应保持 10 不变");
        assertEquals(0, unchanged.getFrozen(), "frozen 应保持 0 不变");
    }

    @Test
    @DisplayName("边界值：扣减数=库存数时成功扣减")
    void decreaseStock_WhenExactAmount_ShouldReturnOne() {
        Integer stockpileId = stockpile.getId();

        // 扣减恰好 10（= 库存数）
        int updatedRows = stockpileRepository.decreaseStock(stockpileId, 10);

        assertEquals(1, updatedRows, "恰好扣完应成功");

        entityManager.clear();
        Stockpile updated = stockpileRepository.findById(stockpileId).orElseThrow();
        assertEquals(0, updated.getAmount(), "amount 应变为 0");
        assertEquals(10, updated.getFrozen(), "frozen 应为 10");
    }

    @Test
    @DisplayName("连续扣减后库存归零，第三次扣减应失败")
    void decreaseStock_TwiceThenExhausted_ThirdFails() {
        Integer id = stockpile.getId();

        // 第一次扣 6
        assertEquals(1, stockpileRepository.decreaseStock(id, 6));
        // 第二次扣 4
        assertEquals(1, stockpileRepository.decreaseStock(id, 4));
        // 第三次：库存已为 0，应失败
        assertEquals(0, stockpileRepository.decreaseStock(id, 1));

        entityManager.clear();
        Stockpile finalState = stockpileRepository.findById(id).orElseThrow();
        assertEquals(0, finalState.getAmount());
        assertEquals(10, finalState.getFrozen());
    }

    @Test
    @DisplayName("单行 UPDATE 的 WHERE 条件可防止超卖（amount >= quantity）")
    void decreaseStock_WhereClause_PreventsOverselling() {
        // 并发模拟：100 个线程同时扣 1（这里顺序验证等价逻辑）
        Integer id = stockpile.getId();
        int successCount = 0;

        for (int i = 0; i < 100; i++) {
            int rows = stockpileRepository.decreaseStock(id, 1);
            if (rows == 1) {
                successCount++;
            }
        }

        // 库存只有 10，成功数应恰好 = 10（WHERE amount >= 1 阻止了后 90 次）
        assertEquals(10, successCount, "100 次扣减只有 10 次应成功（库存上限）");

        entityManager.clear();
        Stockpile finalState = stockpileRepository.findById(id).orElseThrow();
        assertEquals(0, finalState.getAmount(), "最终 amount 应 = 0");
        assertEquals(10, finalState.getFrozen(), "最终 frozen 应 = 10");
    }
}
