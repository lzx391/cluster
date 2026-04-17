package com.cluster.user.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderNotifyListener {

    private static final Logger log = LoggerFactory.getLogger(OrderNotifyListener.class);

    @RabbitListener(queues = "user.order.notify")
    public void onOrderCreated(OrderCreatedMessage message) {
        log.info("[异步] 模拟通知：用户 userId={} 的订单 orderId={} 已创建，金额={}",
                message.userId(), message.orderId(), message.totalAmount());
    }
}
