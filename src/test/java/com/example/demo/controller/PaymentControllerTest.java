package com.example.demo.controller;

import com.example.demo.exception.GlobalExceptionHandler;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.service.PaymentService;
import com.example.demo.vo.payment.PaymentVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock PaymentService paymentService;
    @InjectMocks PaymentController paymentController;

    MockMvc mockMvc;
    private PaymentVO paymentVO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        paymentVO = PaymentVO.builder().paymentId(1L).orderId(1L)
                .orderNumber("ORD-20250101-0001").razorpayOrderId("mock_order_abc123")
                .amount(99999L).currency("INR").status("PENDING")
                .paymentDate(LocalDateTime.now()).build();
    }

    @Test
    void createOrder_success() throws Exception {
        when(paymentService.createOrder("ORD-20250101-0001", "INR")).thenReturn(paymentVO);
        mockMvc.perform(post("/api/payments/create-order")
                        .param("orderNumber", "ORD-20250101-0001").param("currency", "INR"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.razorpayOrderId").value("mock_order_abc123"));
    }

    @Test
    void createOrder_orderNotFound() throws Exception {
        when(paymentService.createOrder(any(), any())).thenThrow(new ResourceNotFoundException("Order not found"));
        mockMvc.perform(post("/api/payments/create-order")
                        .param("orderNumber", "UNKNOWN").param("currency", "INR"))
                .andExpect(status().isNotFound());
    }

    @Test
    void verifyPayment_success() throws Exception {
        paymentVO = PaymentVO.builder().paymentId(1L).orderId(1L)
                .orderNumber("ORD-20250101-0001").razorpayOrderId("mock_order_abc123")
                .razorpayPaymentId("pay_test_123").amount(99999L).currency("INR")
                .status("SUCCESS").paymentDate(LocalDateTime.now()).build();
        when(paymentService.verifyPayment("mock_order_abc123", "pay_test_123", "valid")).thenReturn(paymentVO);
        mockMvc.perform(post("/api/payments/verify")
                        .param("razorpayOrderId", "mock_order_abc123")
                        .param("razorpayPaymentId", "pay_test_123")
                        .param("razorpaySignature", "valid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void verifyPayment_notFound() throws Exception {
        when(paymentService.verifyPayment(any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Payment not found"));
        mockMvc.perform(post("/api/payments/verify")
                        .param("razorpayOrderId", "unknown")
                        .param("razorpayPaymentId", "pay_123")
                        .param("razorpaySignature", "valid"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPayment_success() throws Exception {
        when(paymentService.getPaymentByOrderNumber("ORD-20250101-0001")).thenReturn(paymentVO);
        mockMvc.perform(get("/api/payments/ORD-20250101-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("ORD-20250101-0001"));
    }

    @Test
    void getPayment_notFound() throws Exception {
        when(paymentService.getPaymentByOrderNumber("UNKNOWN"))
                .thenThrow(new ResourceNotFoundException("Not found"));
        mockMvc.perform(get("/api/payments/UNKNOWN"))
                .andExpect(status().isNotFound());
    }

    @Test
    void refundPayment_success() throws Exception {
        when(paymentService.refundPayment("ORD-20250101-0001"))
                .thenReturn(Map.of("status", "success", "message", "Refund initiated"));
        mockMvc.perform(post("/api/payments/ORD-20250101-0001/refund"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void refundPayment_notFound() throws Exception {
        when(paymentService.refundPayment("UNKNOWN"))
                .thenThrow(new ResourceNotFoundException("Not found"));
        mockMvc.perform(post("/api/payments/UNKNOWN/refund"))
                .andExpect(status().isNotFound());
    }
}
