package com.example.tomatomall.Repository;

import com.example.tomatomall.po.Account;
import com.example.tomatomall.po.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {
    List<Cart> findByAccount(Account account);

    Cart findByCartItemId(int cartItemId);
}
