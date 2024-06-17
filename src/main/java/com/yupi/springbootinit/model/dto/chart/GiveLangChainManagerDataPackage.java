package com.yupi.springbootinit.model.dto.chart;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.time.LocalDate;
import org.apache.commons.lang3.builder.HashCodeBuilder;
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
    private Long chartId = -1L;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;


    public int generateUniqueId() {
        // 获取当前日期，不包含时间
        LocalDate today = LocalDate.now();

        return new HashCodeBuilder(17, 37)
                .append(goal)
                .append(csvString)
                .append(today) // 添加日期到哈希生成中
                .toHashCode();
    }
}
