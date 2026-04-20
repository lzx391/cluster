package com.cluster.payment.web;

import com.cluster.payment.service.PaymentService;
import com.cluster.payment.web.dto.CreatePaymentRequest;
import com.cluster.payment.web.dto.PaymentResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * 模拟支付：校验订单为 CREATED 后，调用订单服务将订单置为 PAID，并写入支付流水。
     */
    @PostMapping
    public ResponseEntity<?> pay(@Valid @RequestBody CreatePaymentRequest request) {
        try {
            PaymentResponse body = paymentService.newPay(request.orderId(), request.channel().trim().toUpperCase());
            return ResponseEntity.status(HttpStatus.CREATED).body(body);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }
}
