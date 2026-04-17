package com.cluster.user.messaging;

import java.math.BigDecimal;

/**
 * 与 order-service 发布的 JSON 字段一致，用于演示跨服务异步消息。
 */
public record OrderCreatedMessage(
        Long orderId,
        Long userId,
        Long productId,
        int quantity,
        BigDecimal totalAmount
) {
}
