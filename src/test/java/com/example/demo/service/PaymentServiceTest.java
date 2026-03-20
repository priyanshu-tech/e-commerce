package com.example.demo.service;

import com.example.demo.entity.Order;
import com.example.demo.entity.Payment;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.service.impl.PaymentServiceImpl;
import com.example.demo.vo.payment.PaymentVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock PaymentRepository paymentRepository;
    @InjectMocks PaymentServiceImpl paymentService;

    private Order order;
    private Payment payment;

    @BeforeEach
    void setUp() {
        order = Order.builder().orderId(1L).orderNumber("ORD-20250101-0001")
                .userId(1L).totalAmount(new BigDecimal("999.99"))
                .orderStatus("CONFIRMED").orderDate(LocalDateTime.now())
                .shippingAddressLine1("123 St").shippingCity("Mumbai")
                .shippingState("MH").shippingZipCode("400001").shippingCountry("India").build();

        payment = Payment.builder().paymentId(1L).orderId(1L)
                .orderNumber("ORD-20250101-0001").razorpayOrderId("mock_order_abc123")
                .amount(99999L).currency("INR").status("PENDING")
                .paymentDate(LocalDateTime.now()).build();
    }

    @Test
    void createOrder_success() {
        when(orderRepository.findByOrderNumber("ORD-20250101-0001")).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderNumber("ORD-20250101-0001")).thenReturn(Optional.empty());
        when(paymentRepository.save(any())).thenReturn(payment);

        PaymentVO result = paymentService.createOrder("ORD-20250101-0001", "INR");
        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getRazorpayOrderId()).startsWith("mock_order_");
    }

    @Test
    void createOrder_defaultCurrency() {
        when(orderRepository.findByOrderNumber("ORD-20250101-0001")).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderNumber("ORD-20250101-0001")).thenReturn(Optional.empty());
        when(paymentRepository.save(any())).thenReturn(payment);

        PaymentVO result = paymentService.createOrder("ORD-20250101-0001", null);
        assertThat(result.getCurrency()).isEqualTo("INR");
    }

    @Test
    void createOrder_orderNotFound() {
        when(orderRepository.findByOrderNumber("UNKNOWN")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> paymentService.createOrder("UNKNOWN", "INR"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createOrder_alreadyInitiated() {
        when(orderRepository.findByOrderNumber("ORD-20250101-0001")).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderNumber("ORD-20250101-0001")).thenReturn(Optional.of(payment));
        assertThatThrownBy(() -> paymentService.createOrder("ORD-20250101-0001", "INR"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void verifyPayment_success() {
        when(paymentRepository.findByRazorpayOrderId("mock_order_abc123")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenReturn(payment);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        PaymentVO result = paymentService.verifyPayment("mock_order_abc123", "pay_test_123", "valid");
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        verify(orderRepository).save(any());
    }

    @Test
    void verifyPayment_failed() {
        when(paymentRepository.findByRazorpayOrderId("mock_order_abc123")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenReturn(payment);

        PaymentVO result = paymentService.verifyPayment("mock_order_abc123", "pay_test_123", "invalid");
        assertThat(result.getStatus()).isEqualTo("FAILED");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void verifyPayment_notFound() {
        when(paymentRepository.findByRazorpayOrderId("unknown")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> paymentService.verifyPayment("unknown", "pay_123", "valid"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getPaymentByOrderNumber_success() {
        when(paymentRepository.findByOrderNumber("ORD-20250101-0001")).thenReturn(Optional.of(payment));
        PaymentVO result = paymentService.getPaymentByOrderNumber("ORD-20250101-0001");
        assertThat(result.getOrderNumber()).isEqualTo("ORD-20250101-0001");
    }

    @Test
    void getPaymentByOrderNumber_notFound() {
        when(paymentRepository.findByOrderNumber("UNKNOWN")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> paymentService.getPaymentByOrderNumber("UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void refundPayment_success() {
        payment.setStatus("SUCCESS");
        when(paymentRepository.findByOrderNumber("ORD-20250101-0001")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenReturn(payment);

        Map<String, String> result = paymentService.refundPayment("ORD-20250101-0001");
        assertThat(result.get("status")).isEqualTo("success");
        verify(paymentRepository).save(any());
    }

    @Test
    void refundPayment_notSuccess() {
        when(paymentRepository.findByOrderNumber("ORD-20250101-0001")).thenReturn(Optional.of(payment));
        assertThatThrownBy(() -> paymentService.refundPayment("ORD-20250101-0001"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void refundPayment_notFound() {
        when(paymentRepository.findByOrderNumber("UNKNOWN")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> paymentService.refundPayment("UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
