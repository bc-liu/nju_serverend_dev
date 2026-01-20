package com.example.tomatomall.service.serviceImpl;

import com.example.tomatomall.Repository.OrdersRepository;
import com.example.tomatomall.po.Account;
import com.example.tomatomall.po.Orders;
import com.example.tomatomall.service.OrdersService;
import com.example.tomatomall.utils.SecurityUtil;
import com.example.tomatomall.vo.OrdersVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class OrdersServiceImpl implements OrdersService {

    @Autowired
    SecurityUtil securityUtil;

    @Autowired
    OrdersRepository ordersRepository;

    @Override
    public List<OrdersVO> getPENDINGOrder() {
        Account account = securityUtil.getCurrentUser();
        int userId = account.getId();

        List<Orders> orders = ordersRepository.findByUserId(userId);

        List<OrdersVO> pendingOrders = new ArrayList<>();

        for (Orders order : orders) {
            if(order.getStatus().equals("PENDING")) {
                pendingOrders.add(order.toVO());
            }
        }
        return pendingOrders ;
    }

    @Override
    public void updateOrderSuccess(int orderId) {
        Orders order = ordersRepository.getOrdersByOrderId(orderId);
        order.setStatus("SUCCESS");

        ordersRepository.save(order);
    }


}
