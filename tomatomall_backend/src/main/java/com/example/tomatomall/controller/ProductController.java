package com.example.tomatomall.controller;

import com.example.tomatomall.po.Product;
import com.example.tomatomall.service.ProductService;
import com.example.tomatomall.vo.ProductVO;
import com.example.tomatomall.vo.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products") // 所有接口都以 /api/products 开头
public class ProductController {

    @Autowired
    private ProductService productService;

    // 获取所有商品
    @GetMapping
    public Response<List<ProductVO>> getAllProducts() {
        return Response.buildSuccess(productService.getAllProducts());
    }

    // 根据 ID 获取商品
    @GetMapping("/{id}")
    public Response<ProductVO> getProductById(@PathVariable Integer id) {
        return Response.buildSuccess(productService.getProductById(id));
    }

    // 添加商品
    @PostMapping
    public Response<ProductVO> createProduct(@RequestBody ProductVO productVO) {
        return Response.buildSuccess(productService.createProduct(productVO));
    }

    // 更新商品
    @PutMapping
    public Response<String> updateProduct(@RequestBody ProductVO updatedProduct) {
        return Response.buildSuccess(productService.updateProduct(updatedProduct));
    }

    // 删除商品
    @DeleteMapping("/{id}")
    public Response<String> deleteProduct(@PathVariable Integer id) {
        productService.deleteProduct(id);
        return Response.buildSuccess("删除成功");
    }

    //调整库存
    @PatchMapping("/stockpile/{productId}")
    public Response<String> adjustStockPile(@PathVariable Integer productId, @RequestBody ProductVO.StockpileVO stockpileVO) {
        int amount = stockpileVO.getAmount();
        productService.adjustStockPile(productId, amount);
        return Response.buildSuccess("调整库存成功");
    }

    @GetMapping("/stockpile/{productId}")
    public Response<ProductVO.StockpileVO> getStockPile(@PathVariable Integer productId) {
        return Response.buildSuccess(productService.getStockPile(productId));
    }

    @PostMapping("/stockpile")
    public Response<String> addStockPile(@RequestBody ProductVO.StockpileVO stockpileVO) {
        productService.addStockPile(stockpileVO);
        return Response.buildSuccess("创建库存成功");
    }

    @GetMapping("/myProduct")
    public Response<List<ProductVO>> getMyProduct() {
        return Response.buildSuccess(productService.getMyProduct() );
    }
}
