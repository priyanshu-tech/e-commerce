package com.example.demo.service.impl;

import com.example.demo.entity.Payment;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.service.PaymentService;
import com.example.demo.util.LogUtils;
import com.example.demo.vo.payment.PaymentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public PaymentVO createOrder(String orderNumber, String currency) {
        log.info("Creating mock payment order for orderNumber: {}", orderNumber);

        var order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderNumber));

        if (paymentRepository.findByOrderNumber(orderNumber).isPresent()) {
            throw new IllegalStateException("Payment already initiated for order: " + orderNumber);
        }

        long amountInPaise = order.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue();
        String mockRazorpayOrderId = "mock_order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);

        Payment payment = Payment.builder()
                .orderId(order.getOrderId())
                .orderNumber(orderNumber)
                .razorpayOrderId(mockRazorpayOrderId)
                .amount(amountInPaise)
                .currency(currency != null ? currency : "INR")
                .status("PENDING")
                .build();

        Payment saved = paymentRepository.save(payment);
        PaymentVO result = toVO(saved);
        LogUtils.info(log, "Mock payment order created", result);
        return result;
    }

    @Override
    @Transactional
    public PaymentVO verifyPayment(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        log.info("Verifying mock payment for razorpayOrderId: {}", razorpayOrderId);

        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for razorpayOrderId: " + razorpayOrderId));

        // Mock verification — "valid" signature = success, anything else = failed
        if ("valid".equals(razorpaySignature)) {
            payment.setRazorpayPaymentId(razorpayPaymentId);
            payment.setStatus("SUCCESS");

            orderRepository.findById(payment.getOrderId()).ifPresent(order -> {
                order.setOrderStatus("PAID");
                orderRepository.save(order);
            });
        } else {
            payment.setStatus("FAILED");
        }

        Payment saved = paymentRepository.save(payment);
        PaymentVO result = toVO(saved);
        LogUtils.info(log, "Mock payment verification result", result);
        return result;
    }

    @Override
    public PaymentVO getPaymentByOrderNumber(String orderNumber) {
        log.info("Fetching payment for orderNumber: {}", orderNumber);
        Payment payment = paymentRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order: " + orderNumber));
        PaymentVO result = toVO(payment);
        LogUtils.info(log, "Fetched payment", result);
        return result;
    }

    @Override
    @Transactional
    public Map<String, String> refundPayment(String orderNumber) {
        log.info("Refunding payment for orderNumber: {}", orderNumber);

        Payment payment = paymentRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order: " + orderNumber));

        if (!"SUCCESS".equals(payment.getStatus())) {
            throw new IllegalStateException("Only SUCCESS payments can be refunded. Current status: " + payment.getStatus());
        }

        payment.setStatus("REFUNDED");
        paymentRepository.save(payment);

        log.info("Mock refund completed for orderNumber: {}", orderNumber);
        return Map.of("status", "success", "message", "Refund initiated for order " + orderNumber);
    }

    private PaymentVO toVO(Payment payment) {
        return PaymentVO.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .orderNumber(payment.getOrderNumber())
                .razorpayOrderId(payment.getRazorpayOrderId())
                .razorpayPaymentId(payment.getRazorpayPaymentId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .paymentDate(payment.getPaymentDate())
                .build();
    }
}
