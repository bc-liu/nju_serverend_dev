package com.example.tomatomall.service.serviceImpl;

import com.example.tomatomall.Repository.CartsOrdersRelationRepository;
import com.example.tomatomall.Repository.OrdersRepository;
import com.example.tomatomall.Repository.StockpileRepository;
import com.example.tomatomall.po.Cart;
import com.example.tomatomall.po.Account;
import com.example.tomatomall.po.CartsOrdersRelation;
import com.example.tomatomall.po.Orders;
import com.example.tomatomall.po.Product;
import com.example.tomatomall.po.Stockpile;
import com.example.tomatomall.service.OrdersService;
import com.example.tomatomall.service.StockRedisService;
import com.example.tomatomall.utils.SecurityUtil;
import com.example.tomatomall.vo.OrdersVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class OrdersServiceImpl implements OrdersService {

    @Autowired
    SecurityUtil securityUtil;

    @Autowired
    OrdersRepository ordersRepository;

    @Autowired
    CartsOrdersRelationRepository cartsOrdersRelationRepository;

    @Autowired
    StockpileRepository stockpileRepository;

    @Autowired
    StockRedisService stockRedisService;

    @Override
    public List<OrdersVO> getPENDINGOrder() {
        Account account = securityUtil.getCurrentUser();
        int userId = account.getId();

        List<Orders> orders = ordersRepository.findByUserId(userId);

        List<OrdersVO> pendingOrders = new ArrayList<>();

        for (Orders order : orders) {
            if (order.getStatus().equals("PENDING")) {
                pendingOrders.add(order.toVO());
            }
        }
        return pendingOrders;
    }

    @Override
    public void updateOrderSuccess(int orderId) {
        Orders order = ordersRepository.getOrdersByOrderId(orderId);
        order.setStatus("SUCCESS");

        ordersRepository.save(order);
    }

    @Override
    @Transactional
    public void cancelPendingOrder(int orderId) {
        Orders order = ordersRepository.findById(orderId).orElse(null);
        if (order == null) {
            return;
        }
        if (!"PENDING".equals(order.getStatus())) {
            return;
        }

        List<CartsOrdersRelation> relations = cartsOrdersRelationRepository.findByOrdersOrderId(orderId);
        if (relations != null) {
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
                stockpile.setAmount(stockpile.getAmount() + quantity);
                stockpile.setFrozen(Math.max(stockpile.getFrozen() - quantity, 0));
                stockpileRepository.save(stockpile);
                // 同步刷新 Redis 库存, 保证缓存与 DB 一致
                stockRedisService.refreshStock(product.getId());
            }
            cartsOrdersRelationRepository.deleteAll(relations);
        }

        order.setStatus("CANCELLED");
        ordersRepository.save(order);
    }

}
