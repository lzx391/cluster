package com.cluster.payment.service;

import com.cluster.payment.client.OrderClient;
import com.cluster.payment.config.RabbitConfig;
import com.cluster.payment.domain.Payment;
import com.cluster.payment.messaging.PayOrderMessage;
import com.cluster.payment.repo.PaymentRepository;
import com.cluster.payment.web.dto.PaymentResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import feign.FeignException;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;


import java.time.Instant;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderClient orderClient;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    public PaymentService(PaymentRepository paymentRepository, OrderClient orderClient, RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.orderClient = orderClient;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    public PaymentResponse pay(Long orderId, String channel) {
        OrderClient.OrderDto order;
        try {
            order = orderClient.getOrder(orderId);
        } catch (FeignException e) {
            if (e.status() == 404) {
                throw new IllegalArgumentException("订单不存在");
            }
            throw new IllegalStateException("订单服务暂时不可用", e);
        }

        if (!"CREATED".equals(order.status())) {
            throw new IllegalStateException("订单不可支付（非待支付状态）");
        }

        if (paymentRepository.findByOrderId(orderId).isPresent()) {
            throw new IllegalStateException("该订单已存在支付记录");
        }

        Payment p = new Payment();
        p.setOrderId(orderId);
        p.setAmount(order.totalAmount());
        p.setChannel(channel);
        p.setStatus("PENDING");
        p = paymentRepository.save(p);

        try {
            orderClient.markPaid(orderId, new OrderClient.MarkPaidBody(order.totalAmount()));
        } catch (FeignException e) {
            p.setStatus("FAILED");
            paymentRepository.save(p);
            throw new IllegalStateException("订单确认支付失败", e);
        }

        p.setStatus("SUCCESS");
        p.setPaidAt(Instant.now());
        paymentRepository.save(p);

        return new PaymentResponse(
                p.getId(),
                p.getOrderId(),
                p.getAmount(),
                p.getChannel(),
                p.getStatus(),
                p.getCreatedAt(),
                p.getPaidAt());
    }
    public PaymentResponse newPay(Long orderId, String channel) {
        OrderClient.OrderDto order;
        try {
            order = orderClient.getOrder(orderId);
        } catch (FeignException e) {
            if (e.status() == 404) {
                throw new IllegalArgumentException("订单不存在");
            }
            throw new IllegalStateException("订单服务暂时不可用", e);
        }

        if (!"CREATED".equals(order.status())) {
            throw new IllegalStateException("订单不可支付（非待支付状态）");
        }

        if (paymentRepository.findByOrderId(orderId).isPresent()) {
            throw new IllegalStateException("该订单已存在支付记录");
        }

        Payment p = new Payment();
        p.setOrderId(orderId);
        p.setAmount(order.totalAmount());
        p.setChannel(channel);
        p.setStatus("PENDING");//pending啥意思，答案是待支付
        p = paymentRepository.save(p);

        try {
            //这里改成用消息队列来处理
            // PayOrderMessage msg = new PayOrderMessage(orderId, new OrderClient.MarkPaidBody(order.totalAmount()));
            PayOrderMessage msg = new PayOrderMessage(orderId, order.totalAmount());
            rabbitTemplate.convertAndSend(RabbitConfig.PAYMENT_EXCHANGE, RabbitConfig.PAYMENT_CREATED_ROUTING_KEY, objectMapper.writeValueAsString(msg));
            // orderClient.markPaid(orderId, new OrderClient.MarkPaidBody(order.totalAmount()));
        } catch (JsonProcessingException e) {
            p.setStatus("FAILED");
            paymentRepository.save(p);
            throw new IllegalStateException("订单确认支付失败", e);
        }

        p.setStatus("SUCCESS");
        p.setPaidAt(Instant.now());
        paymentRepository.save(p);

        return new PaymentResponse(
                p.getId(),
                p.getOrderId(),
                p.getAmount(),
                p.getChannel(),
                p.getStatus(),
                p.getCreatedAt(),
                p.getPaidAt());
    }
}
