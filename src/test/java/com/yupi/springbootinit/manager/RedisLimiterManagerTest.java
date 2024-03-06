package com.yupi.springbootinit.manager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class RedisLimiterManagerTest {
    @Resource
    private RedisLimiterManager redisLimiter;
    @Test
    void doLimit() {
        String userID = "1";
        for(int i =0;i<5;i++){
            redisLimiter.doLimit(userID);
            System.out.println("Success") ;
        }
    }
}