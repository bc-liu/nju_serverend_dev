package com.example.tomatomall.kafka;

import com.example.tomatomall.Repository.CartRepository;
import com.example.tomatomall.Repository.CartsOrdersRelationRepository;
import com.example.tomatomall.Repository.OrdersRepository;
import com.example.tomatomall.Repository.StockpileRepository;
import com.example.tomatomall.po.Cart;
import com.example.tomatomall.po.CartsOrdersRelation;
import com.example.tomatomall.po.Orders;
import com.example.tomatomall.po.Product;
import com.example.tomatomall.po.Stockpile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderCreatedConsumer {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private CartsOrdersRelationRepository cartsOrdersRelationRepository;

    @Autowired
    private StockpileRepository stockpileRepository;

    @Autowired
    private CartRepository cartRepository;

    @KafkaListener(topics = "tomatomall.order.created")
    public void onMessage(String message) {
        System.out.println("Kafka received tomatomall.order.created: " + message);
    }

    @KafkaListener(topics = "tomatomall.order.paid")
    public void onOrderPaid(String message) {
        Integer orderId = extractOrderId(message);
        if (orderId == null) {
            return;
        }

        Orders orders = ordersRepository.findById(orderId).orElse(null);
        if (orders == null || !"SUCCESS".equals(orders.getStatus())) {
            return;
        }

        List<CartsOrdersRelation> relations = cartsOrdersRelationRepository.findByOrdersOrderId(orderId);
        if (relations == null || relations.isEmpty()) {
            return;
        }

        for (CartsOrdersRelation relation : relations) {
            Cart cart = relation.getCartItem();
            if (cart == null) {
                continue;
            }
            Product product = cart.getProduct();
            if (product == null) {
                continue;
            }
            Stockpile stockpile = product.getStockpile();
            if (stockpile == null) {
                continue;
            }
            Integer quantity = cart.getQuantity();
            if (quantity == null || quantity <= 0) {
                continue;
            }
            int newFrozen = stockpile.getFrozen() - quantity;
            stockpile.setFrozen(Math.max(newFrozen, 0));
            stockpileRepository.save(stockpile);
        }

        cartsOrdersRelationRepository.deleteAll(relations);
        for (CartsOrdersRelation relation : relations) {
            Cart cart = relation.getCartItem();
            if (cart != null) {
                cartRepository.deleteById(cart.getCartItemId());
            }
        }
    }

    private Integer extractOrderId(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            JsonNode orderIdNode = root.get("orderId");
            if (orderIdNode != null && orderIdNode.isInt()) {
                return orderIdNode.asInt();
            }
            if (orderIdNode != null && orderIdNode.isTextual()) {
                return Integer.parseInt(orderIdNode.asText());
            }
        } catch (Exception ignored) {
        }
        try {
            return Integer.parseInt(message.trim());
        } catch (Exception ignored) {
            return null;
        }
    }
}
