package com.cluster.order.messaging;

import java.math.BigDecimal;

public record PayOrderMessage(
        Long orderId,
        BigDecimal paidAmount) {
}
