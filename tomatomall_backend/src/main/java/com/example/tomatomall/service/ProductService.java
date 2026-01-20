package com.example.tomatomall.service;

import com.example.tomatomall.po.Product;
import com.example.tomatomall.vo.ProductVO;

import java.util.List;

public interface ProductService {
    ProductVO createProduct(ProductVO product);
    ProductVO getProductById(Integer id);
    List<ProductVO> getAllProducts();
    String updateProduct(ProductVO updatedProduct);
    void deleteProduct(Integer id);

    void adjustStockPile(Integer id, Integer amount);
    ProductVO.StockpileVO getStockPile(Integer id);

    void addStockPile(ProductVO.StockpileVO stockpileVO);

    List<ProductVO> getMyProduct();
}
