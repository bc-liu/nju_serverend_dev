package com.example.tomatomall.controller;


import com.example.tomatomall.service.OrdersService;
import com.example.tomatomall.utils.AlipayUtils;
import com.example.tomatomall.vo.OrdersVO;
import com.example.tomatomall.vo.PayResponse;
import com.example.tomatomall.vo.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrdersController {

    @Autowired
    private AlipayUtils alipayUtils;

    @Autowired
    private OrdersService ordersService;

    @PostMapping("/{orderId}/pay")
    public String pay(@PathVariable Integer orderId) {
        PayResponse payResponse = alipayUtils.pay(orderId);
        if (payResponse == null) {
            return "订单已过期或状态不正确";
        }
        return payResponse.getPaymentForm();
    }

    @PostMapping("/notify")
    public String payNotify(HttpServletRequest request) {
        try {
            return alipayUtils.payNotify(request);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    @GetMapping("/returnUrl")
    public String returnUrl() {
        return "支付成功";
    }

    @GetMapping("/pendingOrders")
    public Response<List<OrdersVO>> getPendingOrder() {
        return Response.buildSuccess(ordersService.getPENDINGOrder());
    }

//    @PostMapping("/notify")
//    public void handleAlipayNotify(HttpServletRequest request, HttpServletResponse response) throws IOException, AlipayApiException, IOException {
//        // 1. 解析支付宝回调参数（通常是 application/x-www-form-urlencoded）
//        Map<String, String> params = request.getParameterMap().entrySet().stream()
//                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()[0]));
//
//        // 2. 验证支付宝签名（防止伪造请求）
//        boolean signVerified = AlipaySignature.rsaCheckV1(params, ALIPAY_PUBLIC_KEY, "UTF-8", "RSA2");
//        if (!signVerified) {
//            response.getWriter().print("fail"); // 签名验证失败，返回 fail
//            return;
//        }
//
//        // 3. 处理业务逻辑（更新订单、减库存等）
//        String tradeStatus = params.get("trade_status");
//        if ("TRADE_SUCCESS".equals(tradeStatus)) {
//            String orderId = params.get("out_trade_no"); // 您的订单号
//            String alipayTradeNo = params.get("trade_no"); // 支付宝交易号
//            String amount = params.get("total_amount"); // 支付金额
//
//            // 更新订单状态（注意幂等性，防止重复处理）
//            orderService.updateOrderStatus(orderId, alipayTradeNo, amount);
//
//            // 扣减库存（建议加锁或乐观锁）
//            inventoryService.reduceStock(orderId);
//        }
//
//        // 4. 必须返回纯文本的 "success"（支付宝要求）
//        response.getWriter().print("success");
//    }
}
