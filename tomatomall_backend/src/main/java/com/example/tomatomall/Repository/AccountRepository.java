package com.example.tomatomall.Repository;

import com.example.tomatomall.po.Account;
import com.example.tomatomall.vo.AccountVO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer> {
    Boolean existsByUserName(String userName);

    Account findByName(String name);

    Account findByUserName(String username);
}
