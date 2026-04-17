package com.cluster.product.web.dto;

import java.math.BigDecimal;

public record ProductResponse(Long id, String name, BigDecimal price, int stock) {
}
