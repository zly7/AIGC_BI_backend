package com.yupi.springbootinit.mq;

import com.rabbitmq.client.*;

public class ReceiveLogsDirect {

  private static final String EXCHANGE_NAME = "direct-exchange";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    String queueName = "info-direct-queue";
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();
    channel.queueDeclare(queueName, false, false, false, null);
    channel.queueBind(queueName, EXCHANGE_NAME, "info");

    String queueName2 = "warning-direct-queue";
    channel.queueDeclare(queueName2, false, false, false, null);
    channel.queueBind(queueName2, EXCHANGE_NAME, "warning");



    System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), "UTF-8");
        System.out.println(" [x] Received '" +
            delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
    };
    channel.basicConsume(queueName, true, deliverCallback, consumerTag -> { });
  }
}