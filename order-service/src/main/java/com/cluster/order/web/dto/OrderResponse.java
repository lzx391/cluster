package com.cluster.order.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
        Long id,
        Long userId,
        Long productId,
        int quantity,
        BigDecimal totalAmount,
        String status,
        Instant createdAt
) {
}
