package com.example.demo.controller;

import com.example.demo.service.PaymentService;
import com.example.demo.vo.payment.PaymentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Payment Controller
 * Handles payment processing and transactions
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    @Autowired
    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentVO processPayment(@RequestBody PaymentVO paymentVO) {
        return paymentService.processPayment(paymentVO);
    }

    @GetMapping("/{paymentId}")
    public PaymentVO getPaymentById(@PathVariable Long paymentId) {
        return paymentService.getPaymentById(paymentId);
    }

    @GetMapping("/order/{orderId}")
    public PaymentVO getPaymentByOrderId(@PathVariable Long orderId) {
        return paymentService.getPaymentByOrderId(orderId);
    }

    @PostMapping("/{paymentId}/refund")
    public PaymentVO refundPayment(@PathVariable Long paymentId) {
        return paymentService.refundPayment(paymentId);
    }
}
