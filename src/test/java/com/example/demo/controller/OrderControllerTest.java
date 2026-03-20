package com.example.demo.controller;

import com.example.demo.exception.GlobalExceptionHandler;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.service.OrderService;
import com.example.demo.vo.order.OrderVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock OrderService orderService;
    @InjectMocks OrderController orderController;

    MockMvc mockMvc;
    private OrderVO orderVO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orderController)
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        orderVO = OrderVO.builder().orderId(1L).orderNumber("ORD-20250101-0001")
                .userId(1L).totalAmount(new BigDecimal("1999.98"))
                .orderStatus("CONFIRMED").orderDate(LocalDateTime.now())
                .items(List.of()).shippingAddressLine1("123 St")
                .shippingCity("Mumbai").shippingState("MH")
                .shippingZipCode("400001").shippingCountry("India").build();
    }

    @Test
    void placeOrder_success() throws Exception {
        when(orderService.placeOrder(1L, 1L)).thenReturn(orderVO);
        mockMvc.perform(post("/api/orders").param("userId", "1").param("addressId", "1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").value("ORD-20250101-0001"));
    }

    @Test
    void placeOrder_cartNotFound() throws Exception {
        when(orderService.placeOrder(1L, 1L)).thenThrow(new ResourceNotFoundException("Cart not found"));
        mockMvc.perform(post("/api/orders").param("userId", "1").param("addressId", "1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getOrderByNumber_success() throws Exception {
        when(orderService.getOrderByNumber("ORD-20250101-0001")).thenReturn(orderVO);
        mockMvc.perform(get("/api/orders/ORD-20250101-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("CONFIRMED"));
    }

    @Test
    void getOrderByNumber_notFound() throws Exception {
        when(orderService.getOrderByNumber("UNKNOWN")).thenThrow(new ResourceNotFoundException("Not found"));
        mockMvc.perform(get("/api/orders/UNKNOWN"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUserOrders_success() throws Exception {
        when(orderService.getUserOrders(eq(1L), anyInt(), anyInt())).thenReturn(List.of(orderVO));
        mockMvc.perform(get("/api/orders/user/1").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderNumber").value("ORD-20250101-0001"));
    }

    @Test
    void cancelOrder_success() throws Exception {
        when(orderService.cancelOrder("ORD-20250101-0001"))
                .thenReturn(Map.of("status", "success", "message", "Order cancelled"));
        mockMvc.perform(post("/api/orders/ORD-20250101-0001/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void cancelOrder_notFound() throws Exception {
        when(orderService.cancelOrder("UNKNOWN")).thenThrow(new ResourceNotFoundException("Not found"));
        mockMvc.perform(post("/api/orders/UNKNOWN/cancel"))
                .andExpect(status().isNotFound());
    }
}
