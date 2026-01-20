package com.example.tomatomall.vo;

import com.example.tomatomall.po.Product;
import com.example.tomatomall.po.Specifications;
import com.example.tomatomall.po.Stockpile;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Data
public class ProductVO {
    // 基础信息（对应products表）
    private Integer id;  //商品id
    private String title;   //商品名称
    private BigDecimal price;  // 使用BigDecimal避免精度问题
    private Double rate;  //评分
    private String description;
    private String cover;  //封面url
    private String detail;

    // 规格信息（对应specifications表）
    private List<SpecificationVO> specifications;

    // 库存信息（对应stockpiles表）
    private StockpileVO stockpile;

    // 内部静态类：规格VO
    @Data
    public static class SpecificationVO {
        private Integer id;  //规格id
        private String item;  //规格名称
        private String value;  //规格内容
        private Integer productId;  //所属商品id

        public Specifications toPO() {
            Specifications specification = new Specifications();
            specification.setId(id);
            specification.setItem(item);
            specification.setValue(value);

            return specification;
        }
    }

    // 内部静态类：库存VO
    @Data
    public static class StockpileVO {
        private Integer id;  //库存id
        private Integer productId; //所属
        private Integer amount;     // 可用库存
        private Integer frozen;    // 冻结库存
        private Integer total() {   // 计算总库存
            return amount + frozen;
        }

        public Stockpile toPO() {
            Stockpile stockpile = new Stockpile();
            stockpile.setId(id);
            stockpile.setAmount(amount);
            stockpile.setFrozen(frozen);
//            Integer productId = getProductId();

            return stockpile;
        }
    }


    /**
     *
     * @return productPO，库存不设置
     */
    public Product toPO(){
        Product product = new Product();
        product.setId(id);
        product.setTitle(title);
        product.setPrice(price);
        product.setRate(rate);
        product.setDescription(description);
        product.setCover(cover);
        product.setDetail(detail);

        if(this.specifications != null) {
            List<Specifications> specificationPOs = new ArrayList<>();
            for (SpecificationVO specificationVO : this.specifications) {
                Specifications specification = specificationVO.toPO();
                specification.setProduct(product);
                specificationPOs.add(specification);
            }
            product.setSpecifications(specificationPOs);
        }

//        if(stockpile != null){
//            Stockpile stockpile1 = stockpile.toPO();
//            stockpile1.setProduct(product);
//            product.setStockpile(stockpile1);
//        }

        return product;
    }
}