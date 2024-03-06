package com.yupi.springbootinit.mapper;

import com.yupi.springbootinit.model.entity.Chart;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.HashMap;
import java.util.List;

/**
* @author 26747
* @description 针对表【chart(图表信息表)】的数据库操作Mapper
* @createDate 2024-02-21 09:40:39
* @Entity generator.domain.Chart
*/
public interface ChartMapper extends BaseMapper<Chart> {
    List<HashMap<String,Object>> queryChartData(String sqlLine);
}




