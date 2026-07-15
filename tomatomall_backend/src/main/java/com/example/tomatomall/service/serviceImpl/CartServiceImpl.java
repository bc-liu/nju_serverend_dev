package com.example.tomatomall.service.serviceImpl;

import com.example.tomatomall.Repository.*;
import com.example.tomatomall.exception.TomatoMallException;
import com.example.tomatomall.po.*;
import com.example.tomatomall.service.CartService;
import com.example.tomatomall.service.StockRedisService;
import com.example.tomatomall.utils.SecurityUtil;
import com.example.tomatomall.vo.CartVO;
import com.example.tomatomall.vo.OrdersVO;
import com.example.tomatomall.vo.WholeCart;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartServiceImpl implements CartService {

    private static final Logger log = LoggerFactory.getLogger(CartServiceImpl.class);

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private SecurityUtil securityUtil;
    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private StockpileRepository stockpileRepository;

    @Autowired
    private CartsOrdersRelationRepository cartsOrdersRelationRepository;

    @Autowired
    private StockRedisService stockRedisService;

    @Autowired(required = false)
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public CartVO addToCart(Integer productId, Integer quantity) {
        Product product = productRepository.findById(productId).orElseThrow(TomatoMallException::productNotFound);
        Account account = securityUtil.getCurrentUser();
        Cart cart = new Cart();
        if (product.getStockpile().getAmount() < quantity) {
            throw TomatoMallException.insufficientStock();
        }

        cart.setProduct(product);
        cart.setQuantity(quantity);
        cart.setAccount(account);
        Cart retCart = cartRepository.save(cart);

        return retCart.toVO();
    }

    public void deleteFromCart(Integer cartItemId) {
        cartRepository.findById(cartItemId).orElseThrow(TomatoMallException::cartItemNotFound);
        cartRepository.deleteById(cartItemId);
    }

    public void changeQuantity(Integer cartItemId, Integer quantity) {
        Cart cart = cartRepository.findById(cartItemId).orElseThrow(TomatoMallException::cartItemNotFound);
        cart.setQuantity(quantity);
        cartRepository.save(cart);
    }

    public WholeCart getCartList() {
        Account account = securityUtil.getCurrentUser();
        List<Cart> cartsOfThisAccount = cartRepository.findByAccount(account);

        List<Cart> result = new ArrayList<>();

        for (Cart cart : cartsOfThisAccount) {
            if (!cartsOrdersRelationRepository.existsByCartItem(cart)) {
                result.add(cart);
            }
        }
        WholeCart wholeCart = new WholeCart();
        wholeCart.setCarts(result);
        wholeCart.setTotalAmount();
        wholeCart.setTotal();
        return wholeCart;
    }

    @Transactional
    public OrdersVO checkout(List<Integer> cartItemId, String shoppingAddress, String paymentMethod) {
        if (cartItemId == null || cartItemId.isEmpty()) {
            throw new RuntimeException("购物车商品列表为空");
        }

        Account currentUser = securityUtil.getCurrentUser();

        BigDecimal totalAmount = new BigDecimal(0);

        Orders orders = new Orders();

        List<CartsOrdersRelation> cartsOrdersRelations = new ArrayList<>();

        // 记录已成功扣减的 Redis 库存, 用于事务失败时回滚
        List<int[]> redisDeducted = new ArrayList<>();

        try {
            for (Integer itemId : cartItemId) {
                Cart cart = cartRepository.findById(itemId).orElseThrow(TomatoMallException::cartItemNotFound);
                Product item = productRepository.findById(cart.getProduct().getId())
                        .orElseThrow(TomatoMallException::productNotFound);

                if (item == null) {
                    throw TomatoMallException.cartItemNotFound();
                }

                Stockpile stockpile = item.getStockpile();
                Integer productId = item.getId();
                Integer quantity = cart.getQuantity();

                // 1. Redis + Lua 原子扣减 (主路径, 利用 Redis 单线程避免超卖)
                boolean redisOk = stockRedisService.deductStock(productId, quantity);
                if (!redisOk) {
                    throw TomatoMallException.insufficientStock();
                }
                redisDeducted.add(new int[]{productId, quantity});

                // 2. 同步到数据库, 保证 DB 最终一致
                int updatedRows = stockpileRepository.decreaseStock(stockpile.getId(), quantity);
                if (updatedRows == 0) {
                    // 极端情况: Redis 与 DB 不一致, 回滚 Redis 并失败
                    stockRedisService.restoreStock(productId, quantity);
                    redisDeducted.remove(redisDeducted.size() - 1);
                    throw TomatoMallException.insufficientStock();
                }
                totalAmount = totalAmount.add(item.getPrice().multiply(new BigDecimal(quantity)));

                CartsOrdersRelation cartsOrdersRelation = new CartsOrdersRelation();
                cartsOrdersRelation.setCartItem(cart);
                cartsOrdersRelation.setOrders(orders);
                cartsOrdersRelations.add(cartsOrdersRelation);
            }

            orders.setTotalAmount(totalAmount);
            orders.setUser(currentUser);
            orders.setUserId(currentUser.getId());
            orders.setPaymentMethod(paymentMethod);
            orders.setStatus("PENDING");
            orders.setCreateTime(new Timestamp(System.currentTimeMillis()).toLocalDateTime());
            ordersRepository.save(orders);

            cartsOrdersRelationRepository.saveAll(cartsOrdersRelations);

            Integer orderId = orders.getOrderId();
            List<Integer> cartItemIdsSnapshot = new ArrayList<>(cartItemId);
            CompletableFuture.runAsync(() -> publishOrderCreatedEvent(orderId, currentUser.getId(), orders.getTotalAmount(),
                    paymentMethod, orders.getStatus(), orders.getCreateTime(), shoppingAddress, cartItemIdsSnapshot));

            return orders.toVO();

        } catch (RuntimeException e) {
            // 事务失败: DB 会自动回滚, 这里手动回滚所有已扣减的 Redis 库存, 保证缓存与 DB 一致
            for (int[] d : redisDeducted) {
                try {
                    stockRedisService.restoreStock(d[0], d[1]);
                } catch (Exception ignored) {
                    log.warn("Redis 库存回滚失败: productId={}, quantity={}", d[0], d[1]);
                }
            }
            throw e;
        }
    }

    private void publishOrderCreatedEvent(Integer orderId, Integer userId, BigDecimal totalAmount, String paymentMethod,
            String status, LocalDateTime createTime, String shoppingAddress, List<Integer> cartItemIds) {
        if (kafkaTemplate == null) {
            return;
        }
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "OrderCreated");
            event.put("orderId", orderId);
            event.put("userId", userId);
            event.put("totalAmount", totalAmount);
            event.put("paymentMethod", paymentMethod);
            event.put("status", status);
            event.put("createTime", createTime);
            event.put("shoppingAddress", shoppingAddress);
            event.put("cartItemIds", cartItemIds);
            kafkaTemplate.send("tomatomall.order.created", String.valueOf(orderId),
                    objectMapper.writeValueAsString(event));
        } catch (Exception ignored) {
        }
    }

}
