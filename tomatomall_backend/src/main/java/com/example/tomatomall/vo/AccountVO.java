package com.example.tomatomall.vo;

import com.example.tomatomall.po.Account;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
public class AccountVO {
    private int id;
    private String username;
    private String password;
    private String name;
    private String avatar;
    private String role;
    private String telephone;
    private String email;
    private String location;

    public Account toPO(){
        Account a = new Account();
        a.setId(id);
        a.setUserName(username);
        a.setPassword(password);
        a.setName(name);
        a.setAvatar(avatar);
        a.setRole(role);
        a.setTelephone(telephone);
        a.setEmail(email);
        a.setLocation(location);
        return a;
    }

    public void print(){
        System.out.println(this.username);
        System.out.println(this.password);
        System.out.println(this.name);
        System.out.println(this.avatar);
        System.out.println(this.role);
        System.out.println(this.telephone);
        System.out.println(this.email);
        System.out.println(this.location);

    }

}
