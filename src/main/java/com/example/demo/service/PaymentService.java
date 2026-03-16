package com.example.demo.service;

import com.example.demo.vo.payment.PaymentVO;

/**
 * Payment Service Interface
 * Handles payment processing and transaction business logic
 */
public interface PaymentService {

    PaymentVO processPayment(PaymentVO paymentVO);

    PaymentVO getPaymentById(Long paymentId);

    PaymentVO getPaymentByOrderId(Long orderId);

    PaymentVO refundPayment(Long paymentId);
}
