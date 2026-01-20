package com.example.tomatomall.vo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
@Setter
@Getter
@Data
public class PayResponse {
    private String paymentForm;
    private Integer orderId;
    private BigDecimal totalAmount;
    private String paymentMethod;

}
