package com.example.tomatomall.controller;


import com.alipay.api.domain.OrderVO;
import com.example.tomatomall.service.CartService;
import com.example.tomatomall.vo.CartVO;
import com.example.tomatomall.vo.OrdersVO;
import com.example.tomatomall.vo.Response;
import com.example.tomatomall.vo.WholeCart;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Resource
    private CartService cartService;

    @PostMapping()
    public Response<CartVO> addToCart(@RequestParam Integer productId, @RequestParam Integer quantity) {

        return Response.buildSuccess(cartService.addToCart(productId, quantity));
    }

    @DeleteMapping("/{cartItemId}")
    public Response<String> deleteFromCart(@PathVariable Integer cartItemId){
        cartService.deleteFromCart(cartItemId);
        return Response.buildSuccess("删除成功");
    }

    @PatchMapping("/{cartItemId}")
    public Response<String> changeQuantity(@PathVariable Integer cartItemId, @RequestBody CartVO cartVO) {
        Integer quantity = cartVO.getQuantity();
        cartService.changeQuantity(cartItemId, quantity);
        return Response.buildSuccess("修改数量成功");
    }

    @GetMapping
    public Response<WholeCart> getCartList(){
        return Response.buildSuccess(cartService.getCartList());
    }

    @PostMapping("/checkout")
    public Response<OrdersVO> checkout(@RequestParam List<Integer> cartItemId,
                                       @RequestParam String shoppingAddress,
                                       @RequestParam String paymentMethod) {
        return Response.buildSuccess(cartService.checkout(cartItemId, shoppingAddress, paymentMethod));
    }

}
