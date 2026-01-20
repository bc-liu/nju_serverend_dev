package com.example.tomatomall.utils;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.example.tomatomall.Repository.OrdersRepository;
import com.example.tomatomall.exception.TomatoMallException;
import com.example.tomatomall.po.Orders;
import com.example.tomatomall.service.OrdersService;
import com.example.tomatomall.vo.PayResponse;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.servlet.http.HttpServletRequest;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.alipay.api.AlipayConstants.FORMAT;
import static com.alipay.api.AlipayConstants.FORMAT_JSON;

@Configuration
@ConfigurationProperties(prefix = "alipay")
@Getter
@Setter
public class AlipayUtils {
    private String appId;
    private String privateKey;
    private String AlipayPublicKey;
    private String charset;
    private String signType;
    private String notifyUrl;
    private String returnUrl;
    private String serverUrl;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private OrdersService ordersService;


    public PayResponse pay(Integer orderId){
        Orders orders = ordersRepository.findById(orderId).orElseThrow(TomatoMallException::orderNotFound);

        if(!Objects.equals(orders.getStatus(), "PENDING")){
            return null;
        }

        Long currentTime = System.currentTimeMillis();

        Long createTime = orders.getCreateTime()
                .atZone(ZoneId.systemDefault()) // 使用系统默认时区
                .toInstant()
                .toEpochMilli();


        long diff = currentTime - createTime;

//// 判断是否超过 30 分钟（30 * 60 * 1000 = 1,800,000 毫秒）
//        if (diff > 30 * 60 * 1000L) {
//            ordersRepository.deleteById(orderId);
//            return null;
//        }

        AliPay aliPay = new AliPay();
        aliPay.setTraceNo(String.valueOf(orderId));
        aliPay.setTotalAmount(orders.getTotalAmount().doubleValue());
        aliPay.setSubject("番茄商城订单支付-" + orderId);


        // 1. 创建Client，通用SDK提供的Client，负责调用支付宝的API
        AlipayClient alipayClient = new DefaultAlipayClient(serverUrl, appId,
                privateKey, FORMAT_JSON, charset, AlipayPublicKey, signType);
        // 2. 创建 Request并设置Request参数
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();  // 发送请求的 Request类
        request.setNotifyUrl(notifyUrl);
        String returnUrlWithIdAndAmount = returnUrl + "?" + "orderId=" + aliPay.getTraceNo() + "&" + "amount=" + aliPay.getTotalAmount() ;
        request.setReturnUrl(returnUrlWithIdAndAmount);
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", aliPay.getTraceNo());  // 我们自己生成的订单编号
        bizContent.put("total_amount", String.valueOf(aliPay.getTotalAmount())); // 订单的总金额
        bizContent.put("subject", aliPay.getSubject());   // 支付的名称
        bizContent.put("body", null);
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");  // 固定配置
        request.setBizContent(bizContent.toString());
        // 执行请求，拿到响应的结果，返回给浏览器
        String form = "";
        try {
            form = alipayClient.pageExecute(request).getBody(); // 调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
//        httpResponse.setContentType("text/html;charset=" + charset);
//        httpResponse.getWriter().write(form);// 直接将完整的表单html输出到页面
//        httpResponse.getWriter().flush();
//        httpResponse.getWriter().close();

        PayResponse payResponse = new PayResponse();
        payResponse.setOrderId(orderId);
        payResponse.setTotalAmount(orders.getTotalAmount());
        payResponse.setPaymentMethod("ALIPAY");
        payResponse.setPaymentForm(form);
        System.out.println(payResponse.getOrderId());
        return payResponse;
    }

    public String payNotify(HttpServletRequest request) throws Exception {

        if (request.getParameter("trade_status").equals("TRADE_SUCCESS")) {
            System.out.println("=========支付宝异步回调========");
            Map<String, String> params = new HashMap<>();
            Map<String, String[]> requestParams = request.getParameterMap();
            for (String name : requestParams.keySet()) {
                params.put(name, request.getParameter(name));
                // System.out.println(name + " = " + request.getParameter(name));
            }
            String sign = params.get("sign");
            String content = AlipaySignature.getSignCheckContentV1(params);
            boolean checkSignature = AlipaySignature.rsa256CheckContent(content, sign, AlipayPublicKey, "UTF-8"); // 验证签名
            // 支付宝验签
            if (checkSignature) {
                // 验签通过 可做自己需要的操作
                System.out.println("交易名称: " + params.get("subject"));
                System.out.println("交易状态: " + params.get("trade_status"));
                System.out.println("支付宝交易凭证号: " + params.get("trade_no"));
                System.out.println("商户订单号: " + params.get("out_trade_no"));
                System.out.println("交易金额: " + params.get("total_amount"));
                System.out.println("买家在支付宝唯一id: " + params.get("buyer_id"));
                System.out.println("买家付款时间: " + params.get("gmt_payment"));
                System.out.println("买家付款金额: " + params.get("buyer_pay_amount"));

                //设置订单为SUCCESS
                ordersService.updateOrderSuccess(Integer.parseInt(params.get("out_trade_no")));
            }
        }
        return "success";
    }
}
