package com.yupi.springbootinit.mq;

import com.rabbitmq.client.Channel;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.constant.BiMqConstant;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.manager.AiManager;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.service.ChartService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author bood
 * @since 2024/05/22 15:14
 */
@Component
@Slf4j
public class MyMessageConsumer {

    @Resource
    private ChartService chartService;

    @Resource
    private AiManager aiManager;

    @SneakyThrows
    @RabbitListener(queues = {BiMqConstant.BI_QUEUE_NAME},ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag){
       if(StringUtils.isAnyBlank(message)){
           channel.basicNack(deliveryTag,false,false);
           throw new BusinessException(ErrorCode.SYSTEM_ERROR,"消息为空");
       }
        Long chartId =Long.parseLong(message);
        Chart chart = chartService.getById(chartId);
        if(chart == null){
            channel.basicNack(deliveryTag,false,false);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"图表为空");
        }
        Chart updateChart = new Chart();
        //修改状态为执行中，减少重复执行的风险，执行完成后修改为已完成，执行失败后，修改状态为失败，记录失败信息
        updateChart.setId(chart.getId());
        updateChart.setStatus("running");
        boolean b = chartService.updateById(updateChart);
        if(!b){
            updateChart.setStatus("failed");
            chartService.updateById(updateChart);
            channel.basicNack(deliveryTag,false,false);
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"图表状态更新失败");
        }
        String result = aiManager.doChat(buildUserInput(chart));
        String[] split = result.split("【【【【【");
        if(split.length < 3){
            channel.basicNack(deliveryTag,false,false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"AI 生成错误");
        }
        String genChart = split[1].trim();
        String genResult = split[2].trim();
        Chart updateResultChart = new Chart();
        updateResultChart.setId(chart.getId());
        updateResultChart.setGenResult(genResult);
        updateResultChart.setGenChart(genChart);
        updateResultChart.setStatus("succeed");
        chartService.updateById(updateResultChart);


        //消息确认
        log.info("receiveMessage message = {}",message);
        channel.basicAck(deliveryTag,false);
    }

    /**
     * 构建用户输入
     */
    private String buildUserInput(Chart chart){

        String goal = chart.getGoal();
        String chartType = chart.getChartType();
        //用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求:").append("\n");
        String usergoal = goal;
        if(StringUtils.isNotBlank(chartType)){
            usergoal += ",请使用图表类型为:" + chartType + "的数据格式";
        }
        userInput.append(usergoal).append("\n");
        userInput.append("原始数据:").append("\n");
        return userInput.toString();
    }
}
