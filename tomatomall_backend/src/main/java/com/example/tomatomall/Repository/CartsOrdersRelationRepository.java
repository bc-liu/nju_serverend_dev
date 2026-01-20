package com.example.tomatomall.Repository;

import com.example.tomatomall.po.Cart;
import com.example.tomatomall.po.CartsOrdersRelation;
import com.example.tomatomall.po.Orders;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartsOrdersRelationRepository extends CrudRepository<CartsOrdersRelation, Integer> {
    boolean existsByCartItem(Cart cart);


    List<CartsOrdersRelation> findByOrdersOrderId(Integer orderId);
}
