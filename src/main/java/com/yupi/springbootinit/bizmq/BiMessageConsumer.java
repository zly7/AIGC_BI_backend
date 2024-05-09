package com.yupi.springbootinit.bizmq;

import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.manager.AiManager;
import com.yupi.springbootinit.manager.LangChainManager;
import com.yupi.springbootinit.model.dto.chart.GiveLangChainManagerDataPackage;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.service.ChartService;
import lombok.SneakyThrows;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import com.rabbitmq.client.Channel;
import javax.annotation.Resource;

@Component
public class BiMessageConsumer {
    @Resource
    private ChartService chartService;

    @Resource
    private LangChainManager langChainManager;

    @Resource
    private AiManager aiManager;
    @SneakyThrows
    @RabbitListener(queues = {BiMqConst.BI_QUEUE},
            ackMode = "MANUAL"
    )
    public void receiveMessage(GiveLangChainManagerDataPackage message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        try{
            Chart updatedChart = chartService.getById(message.getChartId());
            updatedChart.setStatus("running");
            boolean updatedResult = chartService.updateById(updatedChart);
            if(!updatedResult){
                handleUpdatedError(updatedChart.getId(),"更新图表错误");
            }
            String answerByAi;
            String modelName = message.getModelName();
            if(modelName.contains("gpt")){
                answerByAi = langChainManager.doLcChat(message);
            } else if (modelName.contains("yucongming")) {
                StringBuilder allPrompt = new StringBuilder();
                allPrompt.append("你是一个数据分析师，现在我会把原始的数据给你，你需要帮我按照要求总结总结。请格式按照要求的#####进行分割，" +
                        "也就是要生成两部分，第一部分是生成图表的前端 Echarts V5 的 option 配置对象js代码，注意要符合JSON的格式，因为之后要用JSON parser解析，第二部分是分析的数据的语言结果，" +
                        "合理地将数据进行可视化，不要生成任何多余的内容。两部分开头都用#####进行开头\n。最后要返回的格式是生成内容(此外不要输出任何多余的开头、结尾、注释):\n" +
                        "#####\n"+
                        "{前端 Echarts V5 的 option 配置对象js代码，合理地将数据进行可视化，Echart JSON 格式里记得为每个字符串附上引号，不要生成任何多余的内容，比如注释,不用markdown格式的```包裹}\n" +
                        "#####\n" +
                        "{明确的数据分析结论、越详细越好，不要生成多余的注释}");
                allPrompt.append("用户要分析的要求是:\n");
                allPrompt.append(message.getGoal()).append("\n");
                allPrompt.append("最后的要生成的图表类型是:\n");
                allPrompt.append(message.getChartType()).append("\n");
                allPrompt.append("原始数据是，这部分是Csv格式，逗号分隔的:\n");
                allPrompt.append(message.getCsvString()).append("\n");
                answerByAi = aiManager.doChat(1654785040361893889L, allPrompt.toString());
            }else {
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"模型名称设置有误");
            }
            String[] splitAnswers = answerByAi.split("#####");
            if(splitAnswers.length!=3){
                updatedChart.setStatus("failed");
                updatedChart.setExecMessage("AI 生成错误,##### 没有准确分割");
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"AI 生成错误,##### 没有准确分割");
            }

            String genChart = splitAnswers[1].trim();
            String genResult = splitAnswers[2].trim();
            updatedChart.setGenResult(genResult);
            updatedChart.setGenChart(genChart);
            updatedChart.setStatus("succeed");
            updatedResult = chartService.updateById(updatedChart);
            if (!updatedResult) {
                handleUpdatedError(updatedChart.getId(), "获得AI数据后，更新图表错误");
                channel.basicNack(tag, false, true); // 消息处理失败，重新放回队列
            } else {
                channel.basicAck(tag, false); // 消息处理成功，确认消息
            }
        }catch (Exception e){
            channel.basicNack(tag, false, false); // 消息处理失败，不放回队列
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
