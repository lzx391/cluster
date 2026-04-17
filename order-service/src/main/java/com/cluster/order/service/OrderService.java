package com.cluster.order.service;

import com.cluster.order.client.ProductClient;
import com.cluster.order.client.UserClient;
import com.cluster.order.domain.Order;
import com.cluster.order.messaging.OrderCreatedMessage;
import com.cluster.order.repo.OrderRepository;
import com.cluster.order.web.dto.CreateOrderRequest;
import com.cluster.order.web.dto.OrderResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import static com.cluster.order.config.RabbitConfig.ORDER_CREATED_ROUTING_KEY;
import static com.cluster.order.config.RabbitConfig.ORDER_EXCHANGE;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserClient userClient;
    private final ProductClient productClient;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public OrderService(
            OrderRepository orderRepository,
            UserClient userClient,
            ProductClient productClient,
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.userClient = userClient;
        this.productClient = productClient;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        try {
            userClient.getUser(request.userId());
        } catch (FeignException e) {
            if (e.status() == 404) {
                throw new IllegalArgumentException("用户不存在");
            }
            throw new IllegalStateException("用户服务暂时不可用", e);
        }

        ProductClient.ProductDto product;
        try {
            product = productClient.getProduct(request.productId());
        } catch (FeignException e) {
            if (e.status() == 404) {
                throw new IllegalArgumentException("商品不存在");
            }
            throw new IllegalStateException("商品服务暂时不可用", e);
        }

        if (product.stock() < request.quantity()) {
            throw new IllegalStateException("库存不足");
        }

        BigDecimal total = product.price()
                .multiply(BigDecimal.valueOf(request.quantity()))
                .setScale(2, RoundingMode.HALF_UP);

        try {
            productClient.deductStock(request.productId(), new ProductClient.DeductBody(request.quantity()));
        } catch (FeignException e) {
            if (e.status() == 400) {
                throw new IllegalStateException("扣减库存失败");
            }
            throw new IllegalStateException("商品服务扣库存失败", e);
        }

        Order o = new Order();
        o.setUserId(request.userId());
        o.setProductId(request.productId());
        o.setQuantity(request.quantity());
        o.setTotalAmount(total);
        Order saved = orderRepository.save(o);

        OrderCreatedMessage msg = new OrderCreatedMessage(
                saved.getId(),
                saved.getUserId(),
                saved.getProductId(),
                saved.getQuantity(),
                saved.getTotalAmount());
        try {
            rabbitTemplate.convertAndSend(ORDER_EXCHANGE, ORDER_CREATED_ROUTING_KEY, objectMapper.writeValueAsString(msg));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("订单已创建但消息序列化失败", e);
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Optional<OrderResponse> findById(Long id) {
        return orderRepository.findById(id).map(OrderService::toResponse);
    }

    @Transactional
    public void markPaid(Long orderId, BigDecimal paidAmount) {
        Order o = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        if (!"CREATED".equals(o.getStatus())) {
            throw new IllegalStateException("订单已支付或已关闭");
        }
        if (o.getTotalAmount().compareTo(paidAmount) != 0) {
            throw new IllegalArgumentException("支付金额与订单金额不一致");
        }
        o.setStatus("PAID");
        orderRepository.save(o);
    }

    private static OrderResponse toResponse(Order o) {
        return new OrderResponse(
                o.getId(),
                o.getUserId(),
                o.getProductId(),
                o.getQuantity(),
                o.getTotalAmount(),
                o.getStatus(),
                o.getCreatedAt());
    }
}
