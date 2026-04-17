package com.cluster.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;

@FeignClient(name = "product-service", contextId = "productClient")
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ProductDto getProduct(@PathVariable("id") Long id);

    @PostMapping("/api/products/{id}/deduct-stock")
    void deductStock(@PathVariable("id") Long id, @RequestBody DeductBody body);

    record ProductDto(Long id, String name, BigDecimal price, int stock) {
    }

    record DeductBody(Integer quantity) {
    }
}
