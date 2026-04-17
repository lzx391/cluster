package com.cluster.user.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderNotifyListener {

    private static final Logger log = LoggerFactory.getLogger(OrderNotifyListener.class);

    private final ObjectMapper objectMapper;

    public OrderNotifyListener(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 消息体为 JSON 字符串（与 order-service 发送方式一致），避免默认序列化 Java 对象失败。
     */
    @RabbitListener(queues = "user.order.notify")
    public void onOrderCreated(String jsonPayload) {
        try {
            OrderCreatedMessage message = objectMapper.readValue(jsonPayload, OrderCreatedMessage.class);
            log.info("[异步] 模拟通知：用户 userId={} 的订单 orderId={} 已创建，金额={}",
                    message.userId(), message.orderId(), message.totalAmount());
        } catch (JsonProcessingException e) {
            log.error("解析订单消息失败: {}", jsonPayload, e);
        }
    }
}
