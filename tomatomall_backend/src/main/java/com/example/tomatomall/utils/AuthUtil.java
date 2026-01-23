package com.example.tomatomall.utils;


import com.example.tomatomall.Repository.AccountRepository;
import com.example.tomatomall.po.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.concurrent.TimeUnit;


@Component
public class AuthUtil {

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    StringRedisTemplate redisTemplate;

    private static final long EXPIRE_TIME = 24 * 60 * 60 * 1000;



    public String getToken(Account user) {
        Date date = new Date(System.currentTimeMillis() + EXPIRE_TIME);
        String token = JWT.create()
                .withAudience(String.valueOf(user.getId()))
                .withExpiresAt(date)
                .sign(Algorithm.HMAC256(user.getPassword()));
        
        // Save token to Redis
        redisTemplate.opsForValue().set("token:" + token, String.valueOf(user.getId()), EXPIRE_TIME, TimeUnit.MILLISECONDS);
        return token;
    }

    public boolean verifyToken(String token) {
        try {
            // Check Redis first
            if (Boolean.FALSE.equals(redisTemplate.hasKey("token:" + token))) {
                return false;
            }
            
            Integer userId=Integer.parseInt(JWT.decode(token).getAudience().get(0));
            Account account= accountRepository.findById(userId).get();
            JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(account.getPassword())).build();
            jwtVerifier.verify(token);
            return true;
        }catch (Exception e){
            return false;
        }
    }

    public Account getAccount(String token){
        Integer userId=Integer.parseInt(JWT.decode(token).getAudience().get(0));
        return accountRepository.findById(userId).get();
    }
}
