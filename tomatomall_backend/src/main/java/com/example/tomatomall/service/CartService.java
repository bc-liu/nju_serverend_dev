package com.example.tomatomall.service;

import com.example.tomatomall.vo.CartVO;
import com.example.tomatomall.vo.OrdersVO;
import com.example.tomatomall.vo.WholeCart;

import java.util.List;

public interface CartService {
    CartVO addToCart(Integer productId, Integer quantity);
    void deleteFromCart(Integer cartItemId);
    void changeQuantity(Integer cartItemId, Integer quantity);
    WholeCart getCartList();
    OrdersVO checkout(List<Integer> cartItemId, String shoppingAddress, String paymentMethod);
}

