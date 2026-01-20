package com.example.tomatomall.exception;

import lombok.Getter;

@Getter
public class TomatoMallException extends RuntimeException {
  private String code;

  public TomatoMallException(String message, String code) {
    super(message);
    this.code = code;
  }

  public static TomatoMallException UsernameAlreadyExists() {
    return new TomatoMallException("用户名已存在", "400");
  }

  public static TomatoMallException notLogin() {
    return new TomatoMallException("未登录", "401");
  }

  public static TomatoMallException nameNotFound() {
    return new TomatoMallException("找不到用户", "404");
  }

  public static TomatoMallException loginFailed() {
    return new TomatoMallException("用户不存在/用户密码错误", "400");
  }

  public static TomatoMallException productNotFound() {
    return new TomatoMallException("商品不存在", "400");
  }

  public static TomatoMallException insufficientStock(){
    return new TomatoMallException("商品库存不足", "409");
  }

  public static TomatoMallException cartItemNotFound(){
    return new TomatoMallException("购物车商品不存在", "400");
  }

  public static TomatoMallException orderNotFound(){
    return new TomatoMallException("找不到订单", "404");
  }

  public static TomatoMallException advertisementsNotFound(){
    return new TomatoMallException("广告不存在", "400");
  }

  public static TomatoMallException passwordNotMatch(){
    return new TomatoMallException("密码错误", "400");
  }
  }
