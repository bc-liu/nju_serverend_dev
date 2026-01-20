package com.example.tomatomall.service;

import com.example.tomatomall.po.Account;
import com.example.tomatomall.vo.AccountVO;

public interface AccountService {

    AccountVO getUser(String username);
    String createUser(AccountVO user);
    String updateUser(AccountVO user);
    String login(String username, String password);
    void updatePassword(String oldPassword, String newPassword);

}
