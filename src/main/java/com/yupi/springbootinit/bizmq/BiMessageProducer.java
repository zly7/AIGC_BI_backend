package com.yupi.springbootinit.bizmq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BiMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public BiMessageProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

//    public void sendMessage(String exchange, String routingKey, Object message) {
//        rabbitTemplate.convertAndSend(exchange, routingKey, message);
//        System.out.println("Sent message: " + message);
//    }
    public void sendMessage(Object message) {
        rabbitTemplate.convertAndSend(BiMqConst.BI_EXCHANGE, BiMqConst.BI_ROUTING_KEY, message);
    }
}
