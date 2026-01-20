package com.example.tomatomall.vo;

import com.example.tomatomall.po.Account;
import com.example.tomatomall.po.Orders;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class OrdersVO {
    private Integer orderId;

    private Integer userId;

    private BigDecimal totalAmount;

    private String paymentMethod;

    private String status;

    private LocalDateTime createTime;

//    private AccountVO user; // 仅包含必要用户信息

    public Orders toPO(Account account) {
        Orders orders = new Orders();
        orders.setOrderId(orderId);
        orders.setUser(account);
        orders.setTotalAmount(totalAmount);
        orders.setPaymentMethod(paymentMethod);
        orders.setStatus(status);
        orders.setCreateTime(createTime);
        return orders;
    }
}
