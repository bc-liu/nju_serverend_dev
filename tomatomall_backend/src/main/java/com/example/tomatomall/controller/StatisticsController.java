package com.example.tomatomall.controller;

import com.example.tomatomall.utils.ApiStatisticsUtil;
import com.example.tomatomall.vo.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    @Autowired
    private ApiStatisticsUtil apiStatisticsUtil;

    /**
     * 获取所有接口的调用统计信息
     */
    @GetMapping("/all")
    public Response<Map<String, Map<String, Object>>> getAllApiStatistics() {
        return Response.buildSuccess(apiStatisticsUtil.getAllApiStatistics());
    }

    /**
     * 获取指定接口的调用统计信息
     */
    @GetMapping("/api")
    public Response<Map<String, Object>> getApiStatistics(
            @RequestParam String className,
            @RequestParam String methodName) {
        return Response.buildSuccess(apiStatisticsUtil.getApiStatistics(className, methodName));
    }
}
