package com.example.tomatomall.po;

import com.example.tomatomall.vo.ProductVO;
import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Data;
import javax.persistence.*;

@Entity
@Table(name = "specifications")
@Data
public class Specifications {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "item", nullable = false, length = 50)
    private String item;

    @Column(name = "value", nullable = false, length = 255)
    private String value;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonBackReference
    private Product product;

    public ProductVO.SpecificationVO toVO(){
        ProductVO.SpecificationVO specificationVO = new ProductVO.SpecificationVO();
        specificationVO.setId(id);
        specificationVO.setItem(item);
        specificationVO.setValue(value);
        specificationVO.setProductId(product.getId());
        return specificationVO;
    }
}