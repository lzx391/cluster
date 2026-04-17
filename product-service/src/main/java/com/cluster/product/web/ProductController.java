package com.cluster.product.web;

import com.cluster.product.service.ProductService;
import com.cluster.product.web.dto.DeductStockRequest;
import com.cluster.product.web.dto.ProductResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductResponse> list() {
        return productService.listAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> get(@PathVariable("id") Long id) {
        ProductResponse p = productService.getById(id);
        if (p == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(p);
    }

    /**
     * 供订单服务在事务边界内扣减库存（工业场景常配合 MQ 做最终一致，此处为演示同步扣减）。
     */
    @PostMapping("/{id}/deduct-stock")
    public ResponseEntity<Void> deduct(@PathVariable("id") Long id, @Valid @RequestBody DeductStockRequest request) {
        try {
            productService.deductStock(id, request);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
