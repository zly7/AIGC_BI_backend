package com.yupi.springbootinit.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
@Configuration
public class ThreadPoolExecutorConfig {

    @Bean
    public ThreadPoolExecutor threadPoolExecutor(){
        ThreadFactory threadFactory = new ThreadFactory() {
            private int count;
            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("thread"+count++);
                return thread;
            }
        };
//        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(2,4,100, //原本的设计，现在是开发阶段不限制线程数
//                TimeUnit.SECONDS,new ArrayBlockingQueue<>(2,true),threadFactory);
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(20,40,100,
                TimeUnit.SECONDS,new ArrayBlockingQueue<>(20,true),threadFactory);
        return threadPoolExecutor;
    }

}
