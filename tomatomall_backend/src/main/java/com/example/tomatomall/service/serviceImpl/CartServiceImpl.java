package com.example.tomatomall.service.serviceImpl;

import com.example.tomatomall.Repository.*;
import com.example.tomatomall.exception.TomatoMallException;
import com.example.tomatomall.po.*;
import com.example.tomatomall.service.CartService;
import com.example.tomatomall.utils.SecurityUtil;
import com.example.tomatomall.vo.CartVO;
import com.example.tomatomall.vo.OrdersVO;
import com.example.tomatomall.vo.WholeCart;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@Service
public class CartServiceImpl implements CartService {

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

    public OrdersVO checkout(List<Integer> cartItemId, String shoppingAddress, String paymentMethod) {
        if (cartItemId == null || cartItemId.isEmpty()) {
            throw new RuntimeException("购物车商品列表为空");
        }

        Account currentUser = securityUtil.getCurrentUser();

        BigDecimal totalAmount = new BigDecimal(0);

        Orders orders = new Orders();

        List<CartsOrdersRelation> cartsOrdersRelations = new ArrayList<>();

        // List<Cart> cartsOfThisAccount = new ArrayList<>();
        for (Integer itemId : cartItemId) {
            Cart cart = cartRepository.findById(itemId).orElseThrow(TomatoMallException::cartItemNotFound);
            // cartsOfThisAccount.add(cart);
            Product item = productRepository.findById(cart.getProduct().getId())
                    .orElseThrow(TomatoMallException::productNotFound);

            if (item == null) {
                throw TomatoMallException.cartItemNotFound();
            }

            Stockpile stockpile = item.getStockpile();
            if (stockpile.getAmount() < cart.getQuantity()) {
                throw TomatoMallException.insufficientStock();
            }
            stockpile.setAmount(stockpile.getAmount() - cart.getQuantity());
            stockpile.setFrozen(stockpile.getFrozen() + cart.getQuantity());
            stockpileRepository.save(stockpile);
            totalAmount = totalAmount.add(item.getPrice().multiply(new BigDecimal(cart.getQuantity())));

            CartsOrdersRelation cartsOrdersRelation = new CartsOrdersRelation();
            cartsOrdersRelation.setCartItem(cart);
            cartsOrdersRelation.setOrders(orders);
            cartsOrdersRelations.add(cartsOrdersRelation);
        }

        // Orders orders = new Orders();
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
