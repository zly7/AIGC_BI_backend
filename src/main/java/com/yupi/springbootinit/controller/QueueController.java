package com.yupi.springbootinit.controller;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.map.HashedMap;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@RestController
@RequestMapping("/queue")
@Slf4j
public class QueueController {
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;
    @GetMapping("/add")
    public void add(String name){
        CompletableFuture.runAsync(() -> {
            log.info("任务执行中: "+name + "执行线程: "+Thread.currentThread().getName());
            try{
                Thread.sleep(600000);
            }catch (InterruptedException e){
                e.printStackTrace();
            }

        },threadPoolExecutor);
    }
    @GetMapping("/get")
    public String get(){
        Map<String,Object> mapInformation = new HashedMap<String,Object>();
        Integer threadNum = threadPoolExecutor.getQueue().size();
        mapInformation.put("队列长度: ",threadNum);
        Long taskCount = threadPoolExecutor.getTaskCount();
        mapInformation.put("任务总数",taskCount);
        int liveThreadNum = threadPoolExecutor.getActiveCount();
        mapInformation.put("正在工作的线程数: ",liveThreadNum);
        Long completedTaskCount = threadPoolExecutor.getCompletedTaskCount();
        mapInformation.put("完成的任务数:",completedTaskCount);
        log.info(mapInformation.toString());
        return JSONUtil.toJsonStr(mapInformation);
    }
}
