package com.cluster.order.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.cluster.order.domain.Order;
import com.cluster.order.repo.OrderRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class PaymentNotifyListener {
    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;

    public PaymentNotifyListener(ObjectMapper objectMapper, OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "user.payment.notify")
    public void onPaymentCreated(String jsonPayload) {
        try {
            PayOrderMessage message = objectMapper.readValue(jsonPayload, PayOrderMessage.class);
            Order o = orderRepository.findById(message.orderId())
                    .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
            if (!"CREATED".equals(o.getStatus())) {
                throw new IllegalStateException("订单已支付或已关闭");
            }
            if (o.getTotalAmount().compareTo(message.paidAmount()) != 0) {
                throw new IllegalArgumentException("支付金额与订单金额不一致");
            }
            o.setStatus("PAID");
            orderRepository.save(o);
            System.out.println("订单已支付");
        } catch (JsonProcessingException e) {
            System.out.println("解析支付消息失败: " + jsonPayload + " " + e);
        }
    }
}
