package com.example.tomatomall.po;

import com.example.tomatomall.vo.AccountVO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class Account {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private int id;

    @Basic
    @Column(name = "username")
    private String userName;

    @Basic
    @Column(name = "password")
    private String password;

    @Basic
    @Column(name = "name")
    private String name;

    @Basic
    @Column(name = "avatar")
    private String avatar;

    @Basic
    @Column(name = "role")
    private String role;

    @Basic
    @Column(name = "telephone")
    private String telephone;

    @Basic
    @Column(name = "email")
    private String email;

    @Basic
    @Column(name = "location")
    private String location;

    public AccountVO toVO() {
        AccountVO vo = new AccountVO();
        vo.setId(id);
        vo.setUsername(userName);
        vo.setName(name);
        vo.setAvatar(avatar);
        vo.setRole(role);
        vo.setTelephone(telephone);
        vo.setEmail(email);
        vo.setLocation(location);
        return vo;
    }

//    /**
//     *
//     * @return 获取用户详情时的vo
//     */
//    public AccountVO toAccountVO(){
//        AccountVO vo = new AccountVO();
//        vo.setUserName(userName);
//        vo.setName(name);
//        vo.setAvatar(avatar);
//        vo.setRole(role);
//        vo.setTelephone(telephone);
//        vo.setEmail(email);
//        vo.setLocation(location);
//        return vo;
//    }


}
