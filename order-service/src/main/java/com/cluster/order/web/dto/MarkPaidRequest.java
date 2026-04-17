package com.cluster.order.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MarkPaidRequest(
        @NotNull @DecimalMin("0.01") BigDecimal paidAmount
) {
}
