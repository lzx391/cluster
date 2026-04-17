package com.cluster.order.service;

import com.cluster.order.client.ProductClient;
import com.cluster.order.client.UserClient;
import com.cluster.order.domain.Order;
import com.cluster.order.messaging.OrderCreatedMessage;
import com.cluster.order.repo.OrderRepository;
import com.cluster.order.web.dto.CreateOrderRequest;
import com.cluster.order.web.dto.OrderResponse;
import feign.FeignException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.cluster.order.config.RabbitConfig.ORDER_CREATED_ROUTING_KEY;
import static com.cluster.order.config.RabbitConfig.ORDER_EXCHANGE;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserClient userClient;
    private final ProductClient productClient;
    private final RabbitTemplate rabbitTemplate;

    public OrderService(
            OrderRepository orderRepository,
            UserClient userClient,
            ProductClient productClient,
            RabbitTemplate rabbitTemplate) {
        this.orderRepository = orderRepository;
        this.userClient = userClient;
        this.productClient = productClient;
        this.rabbitTemplate = rabbitTemplate;
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
        rabbitTemplate.convertAndSend(ORDER_EXCHANGE, ORDER_CREATED_ROUTING_KEY, msg);

        return new OrderResponse(
                saved.getId(),
                saved.getUserId(),
                saved.getProductId(),
                saved.getQuantity(),
                saved.getTotalAmount(),
                saved.getStatus(),
                saved.getCreatedAt());
    }
}
