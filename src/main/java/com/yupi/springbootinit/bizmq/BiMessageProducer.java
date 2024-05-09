package com.yupi.springbootinit.bizmq;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
public class BiMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public BiMessageProducer(RabbitTemplate rabbitTemplateJson) {
        this.rabbitTemplate = rabbitTemplateJson;
    }

    public void sendMessage(Object message) {
        rabbitTemplate.convertAndSend(BiMqConst.BI_EXCHANGE, BiMqConst.BI_ROUTING_KEY, message);
//        System.out.println("Sent JSON message: " + message);
    }
}
