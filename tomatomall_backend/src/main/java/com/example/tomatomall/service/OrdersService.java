package com.example.tomatomall.service;

import com.example.tomatomall.vo.OrdersVO;

import java.util.List;

public interface OrdersService {

    public List<OrdersVO> getPENDINGOrder();

    void updateOrderSuccess(int orderId);
}
