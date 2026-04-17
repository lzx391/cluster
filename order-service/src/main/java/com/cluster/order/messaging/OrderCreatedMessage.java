package com.cluster.order.messaging;

import java.math.BigDecimal;

public record OrderCreatedMessage(
        Long orderId,
        Long userId,
        Long productId,
        int quantity,
        BigDecimal totalAmount
) {
}
