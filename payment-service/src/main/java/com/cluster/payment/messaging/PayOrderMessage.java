package com.cluster.payment.messaging;

import java.math.BigDecimal;

public record PayOrderMessage(   
    Long orderId,
    BigDecimal paidAmount) {
} 