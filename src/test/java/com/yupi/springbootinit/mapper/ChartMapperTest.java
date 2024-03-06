package com.yupi.springbootinit.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class ChartMapperTest {
    @Resource
    private ChartMapper chartMapper;
    @Test
    void queryChartData() {
        List<HashMap<String, Object>> returnList= chartMapper.queryChartData("select * from chart");
        returnList.forEach(item ->System.out.println(item));
        return;
    }
}