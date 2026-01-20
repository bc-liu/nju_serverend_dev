package com.example.tomatomall.vo;


import com.example.tomatomall.po.Account;
import com.example.tomatomall.po.Cart;
import com.example.tomatomall.po.Product;
import com.example.tomatomall.utils.SecurityUtil;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

@Setter
@Getter
@Data
public class CartVO {
    private Integer cartItemId;
    private Integer productId;
    private String title;
    private BigDecimal price;
    private String description;
    private String cover;
    private String detail;
    private Integer quantity;


    public Cart toPO(Account account, Product product) {

        Cart cart = new Cart();
        cart.setCartItemId(this.cartItemId);
        cart.setAccount(account);
        cart.setQuantity(this.quantity);
        cart.setProduct(product);

        return cart;
    }
}
