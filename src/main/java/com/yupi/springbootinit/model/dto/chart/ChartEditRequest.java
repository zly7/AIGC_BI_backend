package com.yupi.springbootinit.model.dto.chart;

import java.io.Serializable;
import java.util.List;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

/**
 * 编辑请求
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Data
public class ChartEditRequest implements Serializable {
    private Long id;
    /**
     * 图表数据
     */
    private String chartData;

    /**
     * 图表名称
     */
    private String name;

    /**
     * 图表类型
     */
    private String chartType;
    /**
     * 分析目标
     */
    private String goal;
    /**
     * 用户id
     */
    private Long usrId;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}