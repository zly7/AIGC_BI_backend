package com.yupi.springbootinit.model.dto.chart;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
@Data
public class GenChartByAiRequest {
    /**
     * 图表类型
     */
    private String chartType;
    /**
     * 分析目标
     */
    private String goal;

    private String name;
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
