package com.yupi.springbootinit.model.dto.chart;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;

@Data
public class GiveLangChainManagerDataPackage implements Serializable {

    /**
     * 图表类型
     */
    private String chartType;
    /**
     * 分析目标
     */
    private String goal;

    private String csvString;

    private String modelName;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
