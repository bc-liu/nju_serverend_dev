package com.example.tomatomall.service;

import com.example.tomatomall.po.Product;
import com.example.tomatomall.vo.ProductVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {
    ProductVO createProduct(ProductVO product);
    ProductVO getProductById(Integer id);
    Page<ProductVO> getAllProducts(Pageable pageable);
    String updateProduct(ProductVO updatedProduct);
    void deleteProduct(Integer id);

    void adjustStockPile(Integer id, Integer amount);
    ProductVO.StockpileVO getStockPile(Integer id);

    void addStockPile(ProductVO.StockpileVO stockpileVO);

    List<ProductVO> getMyProduct();
}
