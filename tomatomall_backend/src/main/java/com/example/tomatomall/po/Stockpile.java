package com.example.tomatomall.po;

import com.example.tomatomall.vo.ProductVO;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.ToString;

import javax.persistence.*;

@Entity
@Table(name = "stockpiles")
@Data
@ToString(exclude = "product")
public class Stockpile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "frozen", nullable = false)
    private Integer frozen;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    @JsonBackReference // ✅ 解决无限递归
    private Product product;

    public ProductVO.StockpileVO toStockpileVO() {
        ProductVO.StockpileVO vo = new ProductVO.StockpileVO();
        vo.setId(id);
        vo.setAmount(amount);
        vo.setFrozen(frozen);
        vo.setProductId(product.getId());
        return vo;
    }
}