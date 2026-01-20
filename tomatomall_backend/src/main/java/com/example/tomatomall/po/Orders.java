package com.example.tomatomall.po;

import com.example.tomatomall.vo.OrdersVO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "Orders")
public class Orders {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Integer orderId;

//    // 外键列（直接映射到数据库字段）
//    @Column(name = "userId", nullable = false)
//    private Integer userId;

    // 外键字段
    @Column(name = "user_id")
    private Integer userId;

    // 多对一关联对象（JPA 关系映射）
    @ManyToOne()
    @JoinColumn(name = "user_id", referencedColumnName = "id",
            insertable = false, updatable = false)
    private Account user;



    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod;

    @Column(name = "status", nullable = false, columnDefinition = "varchar(20) default 'PENDING'")
    private String status = "PENDING"; // 默认值

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    public OrdersVO toVO() {
        OrdersVO vo = new OrdersVO();
        vo.setOrderId(orderId);
        vo.setCreateTime(createTime);
        vo.setPaymentMethod(paymentMethod);
        vo.setTotalAmount(totalAmount);
        vo.setUserId(user.getId());
        vo.setStatus(status);
        return vo;
    }
}
