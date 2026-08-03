package com.example.tomatomall;

import com.example.tomatomall.Repository.CartsOrdersRelationRepository;
import com.example.tomatomall.Repository.OrdersRepository;
import com.example.tomatomall.Repository.StockpileRepository;
import com.example.tomatomall.po.*;
import com.example.tomatomall.service.StockRedisService;
import com.example.tomatomall.service.serviceImpl.OrdersServiceImpl;
import com.example.tomatomall.utils.SecurityUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 订单状态机单元测试 (Mockito)
 * 验证 PENDING → CANCELLED / SUCCESS 的状态流转逻辑
 */
@ExtendWith(MockitoExtension.class)
class OrderStatusMachineTest {

    @Mock
    private SecurityUtil securityUtil;

    @Mock
    private OrdersRepository ordersRepository;

    @Mock
    private CartsOrdersRelationRepository cartsOrdersRelationRepository;

    @Mock
    private StockpileRepository stockpileRepository;

    @Mock
    private StockRedisService stockRedisService;

    @InjectMocks
    private OrdersServiceImpl ordersService;

    // ===== 测试辅助：构造订单对象 =====

    private Orders createPendingOrder(Integer orderId) {
        Account account = new Account();
        account.setId(1);

        Orders order = new Orders();
        order.setOrderId(orderId);
        order.setStatus("PENDING");
        order.setUserId(1);
        order.setUser(account); // toVO() 需要 user.getId()
        order.setTotalAmount(new BigDecimal("99.00"));
        order.setPaymentMethod("ALIPAY");
        return order;
    }

    private Cart createCartItem(Product product, int quantity) {
        Cart cart = new Cart();
        cart.setProduct(product);
        cart.setQuantity(quantity);
        return cart;
    }

    private Product createProductWithStock(int productId, int amount, int frozen) {
        Product product = new Product();
        product.setId(productId);
        product.setTitle("测试商品" + productId);
        product.setPrice(new BigDecimal("99.00"));

        Stockpile stockpile = new Stockpile();
        stockpile.setId(100 + productId); // 不同 product 不同 stockpile id
        stockpile.setAmount(amount);
        stockpile.setFrozen(frozen);
        product.setStockpile(stockpile);

        return product;
    }

    // ============================================================
    // cancelPendingOrder 测试
    // ============================================================

    @Test
    @DisplayName("PENDING 订单取消成功：状态→CANCELLED，库存回滚 amount+1 frozen-1")
    void cancelPendingOrder_PendingOrder_TransitionsToCancelled() {
        // 准备：PENDING 订单，含 1 个商品 quantity=2
        Orders order = createPendingOrder(10);
        Product product = createProductWithStock(1, 8, 2); // amount=8, frozen=2
        Cart cart = createCartItem(product, 2);
        CartsOrdersRelation relation = new CartsOrdersRelation();
        relation.setCartItem(cart);
        relation.setOrders(order);

        when(ordersRepository.findById(10)).thenReturn(Optional.of(order));
        when(cartsOrdersRelationRepository.findByOrdersOrderId(10))
                .thenReturn(Collections.singletonList(relation));

        ordersService.cancelPendingOrder(10);

        // 验证库存回滚：amount 8→10 (+2), frozen 2→0 (-2)
        assertEquals(10, product.getStockpile().getAmount(),
                "取消后 amount 应从 8 恢复到 10（+2）");
        assertEquals(0, product.getStockpile().getFrozen(),
                "取消后 frozen 应从 2 清零（-2）");

        // 验证状态流转
        assertEquals("CANCELLED", order.getStatus(),
                "订单状态应从 PENDING 变为 CANCELLED");

        // 验证持久化调用
        verify(stockpileRepository).save(any(Stockpile.class));
        verify(stockRedisService).refreshStock(1);
        verify(cartsOrdersRelationRepository).deleteAll(any());
        verify(ordersRepository).save(order);
    }

    @Test
    @DisplayName("订单不存在时静默返回，不抛异常")
    void cancelPendingOrder_OrderNotFound_ReturnsGracefully() {
        when(ordersRepository.findById(999)).thenReturn(Optional.empty());

        // 不应抛异常
        assertDoesNotThrow(() -> ordersService.cancelPendingOrder(999));

        // 不应调用任何后续操作
        verify(cartsOrdersRelationRepository, never()).findByOrdersOrderId(any());
        verify(ordersRepository, never()).save(any());
    }

    @Test
    @DisplayName("非 PENDING 状态订单取消失败，状态不变")
    void cancelPendingOrder_NonPendingOrder_NoChange() {
        Orders order = createPendingOrder(10);
        order.setStatus("SUCCESS"); // 已支付

        when(ordersRepository.findById(10)).thenReturn(Optional.of(order));

        ordersService.cancelPendingOrder(10);

        // 状态不应改变
        assertEquals("SUCCESS", order.getStatus());

        // 不应修改库存
        verify(cartsOrdersRelationRepository, never()).findByOrdersOrderId(any());
        verify(ordersRepository, never()).save(any());
    }

    @Test
    @DisplayName("取消订单时回滚所有关联商品库存")
    void cancelPendingOrder_MultipleCartItems_RestoresAllStock() {
        Orders order = createPendingOrder(10);

        Product product1 = createProductWithStock(1, 5, 5); // amount=5, frozen=5
        Product product2 = createProductWithStock(2, 10, 3); // amount=10, frozen=3

        Cart cart1 = createCartItem(product1, 3);  // qty=3
        Cart cart2 = createCartItem(product2, 2);  // qty=2

        CartsOrdersRelation rel1 = new CartsOrdersRelation();
        rel1.setCartItem(cart1);
        CartsOrdersRelation rel2 = new CartsOrdersRelation();
        rel2.setCartItem(cart2);

        when(ordersRepository.findById(10)).thenReturn(Optional.of(order));
        when(cartsOrdersRelationRepository.findByOrdersOrderId(10))
                .thenReturn(Arrays.asList(rel1, rel2));

        ordersService.cancelPendingOrder(10);

        // product1: amount 5→8, frozen 5→2
        assertEquals(8, product1.getStockpile().getAmount());
        assertEquals(2, product1.getStockpile().getFrozen());

        // product2: amount 10→12, frozen 3→1
        assertEquals(12, product2.getStockpile().getAmount());
        assertEquals(1, product2.getStockpile().getFrozen());

        // 每个商品的库存都要刷新（productId=1 和 productId=2）
        verify(stockRedisService).refreshStock(1);
        verify(stockRedisService).refreshStock(2);
        verify(stockRedisService, times(2)).refreshStock(any());
    }

    @Test
    @DisplayName("取消订单后 frozen 不会为负（Math.max 防护）")
    void cancelPendingOrder_FrozenWontGoNegative() {
        // 异常场景：frozen 已经 < quantity（数据不一致）
        Orders order = createPendingOrder(10);
        Product product = createProductWithStock(1, 8, 0); // frozen 异常=0
        Cart cart = createCartItem(product, 5); // quantity=5 > frozen=0
        CartsOrdersRelation relation = new CartsOrdersRelation();
        relation.setCartItem(cart);

        when(ordersRepository.findById(10)).thenReturn(Optional.of(order));
        when(cartsOrdersRelationRepository.findByOrdersOrderId(10))
                .thenReturn(Collections.singletonList(relation));

        ordersService.cancelPendingOrder(10);

        // frozen 被 Math.max 保护，不会为负
        assertTrue(product.getStockpile().getFrozen() >= 0,
                "frozen 不应小于 0（Math.max 兜底）");
    }

    // ============================================================
    // updateOrderSuccess 测试
    // ============================================================

    @Test
    @DisplayName("支付成功：PENDING → SUCCESS 状态流转")
    void updateOrderSuccess_PendingOrder_TransitionsToSuccess() {
        Orders order = createPendingOrder(5);

        when(ordersRepository.getOrdersByOrderId(5)).thenReturn(order);

        ordersService.updateOrderSuccess(5);

        assertEquals("SUCCESS", order.getStatus(),
                "订单状态应从 PENDING 变为 SUCCESS");
        verify(ordersRepository).save(order);
    }

    // ============================================================
    // getPENDINGOrder 测试
    // ============================================================

    @Test
    @DisplayName("只返回当前用户的 PENDING 订单，过滤非 PENDING")
    void getPendingOrder_ReturnsOnlyPendingForCurrentUser() {
        Account account = new Account();
        account.setId(8);

        Orders pending1 = createPendingOrder(1);
        Orders pending2 = createPendingOrder(2);
        Orders paid = createPendingOrder(3);
        paid.setStatus("SUCCESS"); // 已支付，不应返回

        when(securityUtil.getCurrentUser()).thenReturn(account);
        when(ordersRepository.findByUserId(8))
                .thenReturn(Arrays.asList(pending1, pending2, paid));

        List<?> result = ordersService.getPENDINGOrder();

        assertEquals(2, result.size(),
                "应只返回 2 个 PENDING 订单，过滤掉 SUCCESS 的");
    }
}
