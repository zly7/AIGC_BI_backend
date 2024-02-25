package com.yupi.springbootinit.manager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@SpringBootTest
class AiManagerTest {
    @Resource
    private AiManager aiManager;
    @Test
    void doChatTest() {
        String messageAnswer = this.aiManager.doChat(1654785040361893889L,"清华北大谁更好？");
        System.out.println(messageAnswer);
    }
}