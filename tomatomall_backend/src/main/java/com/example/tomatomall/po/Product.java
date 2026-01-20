package com.example.tomatomall.po;

import com.example.tomatomall.po.Stockpile;
import com.example.tomatomall.vo.ProductVO;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Data
@ToString(exclude = {"stockpile", "specifications"})

public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "title", nullable = false, length = 50)
    private String title;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "rate", nullable = false)
    private Double rate;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "cover", length = 500)
    private String cover;

    @Column(name = "detail", length = 500)
    private String detail;

    // 一对多关联规格表
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @JsonIgnore
    private List<Specifications> specifications;

    // 一对一关联库存表
    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @JsonIgnore
    private Stockpile stockpile;


    /**
     *
     * @return 返回值中的vo不包括库存表
     */
    public ProductVO toVO() {
        ProductVO productVO = new ProductVO();
        productVO.setId(id);
        productVO.setTitle(title);
        productVO.setPrice(price);
        productVO.setRate(rate);
        productVO.setDescription(description);
        productVO.setCover(cover);
        productVO.setDetail(detail);

        if(stockpile != null) {
            productVO.setStockpile(stockpile.toStockpileVO());
        }
        List<ProductVO.SpecificationVO> specificationVOs = new ArrayList<>();
        if(specifications != null) {
            for (Specifications specification : specifications) {
                specificationVOs.add(specification.toVO());
            }
            productVO.setSpecifications(specificationVOs);
        }
//        productVO.setStockpile(stockpile.toStockpileVO());
        return productVO;
    }

    // 辅助方法：添加规格
    public void addSpecification(Specifications specification) {
        this.specifications.add(specification);
        if(specification != null){
            specification.setProduct(this);
        }

    }

    // 辅助方法：设置库存
    public void setStockpile(Stockpile stockpile) {
        this.stockpile = stockpile;
        if(stockpile != null){
            stockpile.setProduct(this);
        }

    }
}