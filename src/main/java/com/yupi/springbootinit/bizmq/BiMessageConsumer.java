package com.yupi.springbootinit.bizmq;

import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.manager.AiManager;
import com.yupi.springbootinit.manager.LangChainManager;
import com.yupi.springbootinit.model.dto.chart.GiveLangChainManagerDataPackage;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.service.ChartService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class BiMessageConsumer {
    @Resource
    private ChartService chartService;

    @Resource
    private LangChainManager langChainManager;

    @Resource
    private AiManager aiManager;

    @RabbitListener(queues = BiMqConst.BI_QUEUE)
    public void receiveMessage(Object message) {
        GiveLangChainManagerDataPackage giveLangChainManagerDataPackage = (GiveLangChainManagerDataPackage) message;
        Chart updatedChart = chartService.getById(giveLangChainManagerDataPackage.getChartId());
        updatedChart.setStatus("running");
        String answerByAi;
        String modelName = giveLangChainManagerDataPackage.getModelName();
        if(modelName.contains("gpt")){
            answerByAi = langChainManager.doLcChat(giveLangChainManagerDataPackage);
        } else if (modelName.contains("yucongming")) {
            StringBuilder allPrompt = new StringBuilder();
            allPrompt.append("你是一个数据分析师，现在我会把原始的数据给你，你需要帮我按照要求总结总结。请格式按照要求的#####进行分割，" +
                    "也就是要生成两部分，第一部分是生成图表的前端 Echarts V5 的 option 配置对象is代码，第二部分是分析的数据的语言结果，" +
                    "合理地将数据进行可视化，不要生成任何多余的内容。两部分开头都用#####进行开头\n。最后要返回的格式是生成内容(此外不要输出任何多余的开头、结尾、注释):\n" +
                    "#####\n"+
                    "{前端 Echarts V5 的 option 配置对象js代码，合理地将数据进行可视化，Echart JSON 格式里记得为每个字符串附上引号，不要生成任何多余的内容，比如注释,不用markdown格式的```包裹}\n" +
                    "#####\n" +
                    "{明确的数据分析结论、越详细越好，不要生成多余的注释}");
            allPrompt.append("用户要分析的要求是:\n");
            allPrompt.append(giveLangChainManagerDataPackage.getGoal()).append("\n");
            allPrompt.append("最后的要生成的图表类型是:\n");
            allPrompt.append(giveLangChainManagerDataPackage.getChartType()).append("\n");
            allPrompt.append("原始数据是，这部分是Csv格式，逗号分隔的:\n");
            allPrompt.append(giveLangChainManagerDataPackage.getCsvString()).append("\n");
            answerByAi = aiManager.doChat(1654785040361893889L, allPrompt.toString());
        }else {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"模型名称设置有误");
        }
        String[] splitAnswers = answerByAi.split("#####");
        if(splitAnswers.length!=3){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"AI 生成错误,##### 没有准确分割");
        }

        String genChart = splitAnswers[1].trim();
        String genResult = splitAnswers[2].trim();
        updatedChart.setGenResult(genResult);
        updatedChart.setGenChart(genChart);
        updatedChart.setStatus("succeed");
        boolean updatedResult = chartService.updateById(updatedChart);
        if(!updatedResult){
            handleUpdatedError(updatedChart.getId(),"获得AI数据后，更新图表错误");
        }

    }
    private void handleUpdatedError(Long chartId,String execMessage){
        Chart newChart = new Chart();
        newChart.setId(chartId);
        newChart.setStatus("failed");
        newChart.setExecMessage(execMessage);
        boolean updatedResult = chartService.updateById(newChart);
        return;
    }
}
