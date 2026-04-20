package com.cluster.order.config;

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

    
    public static final String PAYMENT_EXCHANGE = "payment.exchange";
    public static final String PAYMENT_CREATED_ROUTING_KEY = "payment.created";
    public static final String USER_PAYMENT_NOTIFY_QUEUE = "user.payment.notify";

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE, true, false);
    }
    
    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE, true, false);
    }
    @Bean
    public Queue userPaymentNotifyQueue() {
        return new Queue(USER_PAYMENT_NOTIFY_QUEUE, true);
    }
    @Bean
    public Binding userPaymentNotifyBinding(Queue userPaymentNotifyQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(userPaymentNotifyQueue).to(paymentExchange).with(PAYMENT_CREATED_ROUTING_KEY);
    }
}
