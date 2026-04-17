package com.cluster.payment.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long id,
        Long orderId,
        BigDecimal amount,
        String channel,
        String status,
        Instant createdAt,
        Instant paidAt
) {
}
