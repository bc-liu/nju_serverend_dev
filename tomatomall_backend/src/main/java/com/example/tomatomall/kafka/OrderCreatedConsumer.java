package com.example.tomatomall.kafka;

import com.example.tomatomall.Repository.CartRepository;
import com.example.tomatomall.Repository.CartsOrdersRelationRepository;
import com.example.tomatomall.Repository.KafkaEventLogRepository;
import com.example.tomatomall.Repository.OrdersRepository;
import com.example.tomatomall.Repository.StockpileRepository;
import com.example.tomatomall.po.Cart;
import com.example.tomatomall.po.CartsOrdersRelation;
import com.example.tomatomall.po.KafkaEventLog;
import com.example.tomatomall.po.Orders;
import com.example.tomatomall.po.Product;
import com.example.tomatomall.po.Stockpile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class OrderCreatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedConsumer.class);

    private static final String EVENT_TYPE_ORDER_CREATED = "OrderCreated";
    private static final String EVENT_TYPE_ORDER_PAID = "OrderPaid";

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

    @Autowired
    private KafkaEventLogRepository kafkaEventLogRepository;

    @KafkaListener(topics = "tomatomall.order.created", groupId = "order-created-group")
    public void onMessage(@Payload String message,
            @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        log.info("Kafka received tomatomall.order.created: partition={}, offset={}, message={}",
                partition, offset, message);
        acknowledgment.acknowledge();
    }

    @KafkaListener(topics = "tomatomall.order.paid", groupId = "order-paid-group")
    public void onOrderPaid(@Payload String message,
            @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        Integer orderId = extractOrderId(message);
        if (orderId == null) {
            log.warn("无法解析orderId，消息内容: {}", message);
            acknowledgment.acknowledge();
            return;
        }

        String eventKey = String.valueOf(orderId);

        try {
            boolean alreadyProcessed = kafkaEventLogRepository
                    .existsByEventTypeAndEventKey(EVENT_TYPE_ORDER_PAID, eventKey);

            if (alreadyProcessed) {
                log.info("订单支付事件已处理过，跳过: orderId={}", orderId);
                acknowledgment.acknowledge();
                return;
            }

            KafkaEventLog eventLog = new KafkaEventLog(EVENT_TYPE_ORDER_PAID, eventKey, message);
            kafkaEventLogRepository.save(eventLog);

            processOrderPayment(orderId);

            eventLog.markAsSuccess();
            kafkaEventLogRepository.save(eventLog);

            acknowledgment.acknowledge();
            log.info("订单支付处理成功: orderId={}, partition={}, offset={}", orderId, partition, offset);

        } catch (OptimisticLockingFailureException e) {
            log.warn("并发处理检测到，跳过: orderId={}", orderId);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("订单支付处理失败: orderId={}, error={}", orderId, e.getMessage(), e);
            try {
                KafkaEventLog eventLog = kafkaEventLogRepository
                        .findByEventTypeAndEventKey(EVENT_TYPE_ORDER_PAID, eventKey)
                        .orElse(null);
                if (eventLog != null) {
                    eventLog.markAsFailed(e.getMessage());
                    kafkaEventLogRepository.save(eventLog);
                }
            } catch (Exception ex) {
                log.error("记录失败日志异常", ex);
            }
            throw new RuntimeException("业务处理失败，触发重试", e);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    protected void processOrderPayment(Integer orderId) {
        Orders orders = ordersRepository.findById(orderId).orElse(null);
        if (orders == null || !"SUCCESS".equals(orders.getStatus())) {
            throw new RuntimeException("订单不存在或状态不正确: " + orderId);
        }

        List<CartsOrdersRelation> relations = cartsOrdersRelationRepository.findByOrdersOrderId(orderId);
        if (relations == null || relations.isEmpty()) {
            log.warn("订单没有关联的购物车项: orderId={}", orderId);
            return;
        }

        for (CartsOrdersRelation relation : relations) {
            Cart cart = relation.getCartItem();
            if (cart == null)
                continue;

            Product product = cart.getProduct();
            if (product == null)
                continue;

            Stockpile stockpile = product.getStockpile();
            if (stockpile == null)
                continue;

            Integer quantity = cart.getQuantity();
            if (quantity == null || quantity <= 0)
                continue;

            int newFrozen = stockpile.getFrozen() - quantity;
            stockpile.setFrozen(Math.max(newFrozen, 0));
            stockpileRepository.save(stockpile);

            log.debug("库存解冻: productId={}, frozen变化={} -> {}",
                    product.getId(), stockpile.getFrozen() + quantity, stockpile.getFrozen());
        }

        cartsOrdersRelationRepository.deleteAll(relations);

        for (CartsOrdersRelation relation : relations) {
            Cart cart = relation.getCartItem();
            if (cart != null) {
                cartRepository.deleteById(cart.getCartItemId());
                log.debug("删除购物车项: cartItemId={}", cart.getCartItemId());
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
