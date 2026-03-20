package com.example.demo.service.impl;

import com.example.demo.entity.Order;
import com.example.demo.entity.Payment;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.vo.payment.PaymentVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock OrderRepository orderRepository;
    @Mock PaymentRepository paymentRepository;
    @InjectMocks PaymentServiceImpl paymentService;

    private Order order() {
        return Order.builder().orderId(1L).orderNumber("ORD-20260101-0001")
                .userId(1L).totalAmount(new BigDecimal("999.99"))
                .orderStatus("CONFIRMED").build();
    }

    private Payment pendingPayment() {
        return Payment.builder().paymentId(1L).orderId(1L)
                .orderNumber("ORD-20260101-0001")
                .razorpayOrderId("mock_order_abc123")
                .amount(99999L).currency("INR").status("PENDING")
                .paymentDate(LocalDateTime.now()).build();
    }

    private Payment successPayment() {
        return Payment.builder().paymentId(1L).orderId(1L)
                .orderNumber("ORD-20260101-0001")
                .razorpayOrderId("mock_order_abc123")
                .razorpayPaymentId("mock_pay_xyz")
                .amount(99999L).currency("INR").status("SUCCESS")
                .paymentDate(LocalDateTime.now()).build();
    }

    // ── createOrder ──────────────────────────────────────────

    @Test
    void createOrder_success() {
        when(orderRepository.findByOrderNumber("ORD-20260101-0001")).thenReturn(Optional.of(order()));
        when(paymentRepository.findByOrderNumber("ORD-20260101-0001")).thenReturn(Optional.empty());
        when(paymentRepository.save(any())).thenReturn(pendingPayment());

        PaymentVO result = paymentService.createOrder("ORD-20260101-0001", "INR");

        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getRazorpayOrderId()).startsWith("mock_order_");
        assertThat(result.getAmount()).isEqualTo(99999L);
    }

    @Test
    void createOrder_orderNotFound_throws() {
        when(orderRepository.findByOrderNumber("INVALID")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.createOrder("INVALID", "INR"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createOrder_alreadyInitiated_throws() {
        when(orderRepository.findByOrderNumber("ORD-20260101-0001")).thenReturn(Optional.of(order()));
        when(paymentRepository.findByOrderNumber("ORD-20260101-0001")).thenReturn(Optional.of(pendingPayment()));

        assertThatThrownBy(() -> paymentService.createOrder("ORD-20260101-0001", "INR"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already initiated");
    }

    // ── verifyPayment ────────────────────────────────────────

    @Test
    void verifyPayment_validSignature_success() {
        Payment payment = pendingPayment();
        Order order = order();

        when(paymentRepository.findByRazorpayOrderId("mock_order_abc123")).thenReturn(Optional.of(payment));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any())).thenReturn(payment);

        PaymentVO result = paymentService.verifyPayment("mock_order_abc123", "mock_pay_xyz", "valid");

        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        verify(orderRepository).save(argThat(o -> "PAID".equals(o.getOrderStatus())));
    }

    @Test
    void verifyPayment_invalidSignature_failed() {
        Payment payment = pendingPayment();

        when(paymentRepository.findByRazorpayOrderId("mock_order_abc123")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenReturn(payment);

        PaymentVO result = paymentService.verifyPayment("mock_order_abc123", "mock_pay_xyz", "invalid");

        assertThat(result.getStatus()).isEqualTo("FAILED");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void verifyPayment_notFound_throws() {
        when(paymentRepository.findByRazorpayOrderId("INVALID")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.verifyPayment("INVALID", "pay_xyz", "valid"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── refundPayment ────────────────────────────────────────

    @Test
    void refundPayment_success() {
        when(paymentRepository.findByOrderNumber("ORD-20260101-0001")).thenReturn(Optional.of(successPayment()));
        when(paymentRepository.save(any())).thenReturn(successPayment());

        var result = paymentService.refundPayment("ORD-20260101-0001");

        assertThat(result.get("status")).isEqualTo("success");
        verify(paymentRepository).save(argThat(p -> "REFUNDED".equals(p.getStatus())));
    }

    @Test
    void refundPayment_notSuccess_throws() {
        when(paymentRepository.findByOrderNumber("ORD-20260101-0001")).thenReturn(Optional.of(pendingPayment()));

        assertThatThrownBy(() -> paymentService.refundPayment("ORD-20260101-0001"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only SUCCESS");
    }

    @Test
    void refundPayment_notFound_throws() {
        when(paymentRepository.findByOrderNumber("INVALID")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.refundPayment("INVALID"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getPaymentByOrderNumber ──────────────────────────────

    @Test
    void getPaymentByOrderNumber_success() {
        when(paymentRepository.findByOrderNumber("ORD-20260101-0001")).thenReturn(Optional.of(successPayment()));

        PaymentVO result = paymentService.getPaymentByOrderNumber("ORD-20260101-0001");

        assertThat(result.getOrderNumber()).isEqualTo("ORD-20260101-0001");
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void getPaymentByOrderNumber_notFound_throws() {
        when(paymentRepository.findByOrderNumber("INVALID")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentByOrderNumber("INVALID"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
