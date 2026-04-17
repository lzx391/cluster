package com.cluster.product.service;

import com.cluster.product.domain.Product;
import com.cluster.product.repo.ProductRepository;
import com.cluster.product.web.dto.DeductStockRequest;
import com.cluster.product.web.dto.ProductResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Cacheable(cacheNames = "productList")
    @Transactional(readOnly = true)
    public List<ProductResponse> listAll() {
        return productRepository.findAll().stream().map(ProductService::toResponse).toList();
    }

    @Cacheable(cacheNames = "products", key = "#id", unless = "#result == null")
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return productRepository.findById(id).map(ProductService::toResponse).orElse(null);
    }

    @Transactional
    @CacheEvict(cacheNames = {"products", "productList"}, allEntries = true)
    public void deductStock(Long productId, DeductStockRequest request) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在: " + productId));
        if (p.getStock() < request.quantity()) {
            throw new IllegalStateException("库存不足");
        }
        p.setStock(p.getStock() - request.quantity());
        productRepository.save(p);
    }

    private static ProductResponse toResponse(Product p) {
        return new ProductResponse(p.getId(), p.getName(), p.getPrice(), p.getStock());
    }
}
