package com.cluster.user.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String USER_ORDER_NOTIFY_QUEUE = "user.order.notify";

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE, true, false);
    }

    @Bean
    public Queue userOrderNotifyQueue() {
        return new Queue(USER_ORDER_NOTIFY_QUEUE, true);
    }

    @Bean
    public Binding userOrderNotifyBinding(Queue userOrderNotifyQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(userOrderNotifyQueue).to(orderExchange).with(ORDER_CREATED_ROUTING_KEY);
    }
}
