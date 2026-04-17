package com.cluster.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.time.Instant;

@FeignClient(name = "order-service", contextId = "orderClient")
public interface OrderClient {

    @GetMapping("/api/orders/{id}")
    OrderDto getOrder(@PathVariable("id") Long id);

    @PostMapping("/api/orders/{id}/pay")
    void markPaid(@PathVariable("id") Long id, @RequestBody MarkPaidBody body);

    record OrderDto(
            Long id,
            Long userId,
            Long productId,
            int quantity,
            BigDecimal totalAmount,
            String status,
            Instant createdAt
    ) {
    }

    record MarkPaidBody(BigDecimal paidAmount) {
    }
}
