package com.example.demo.controller;

import com.example.demo.exception.GlobalExceptionHandler;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.service.CartService;
import com.example.demo.vo.cart.CartItemVO;
import com.example.demo.vo.cart.CartVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock CartService cartService;
    @InjectMocks CartController cartController;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();
    private CartVO cartVO;
    private CartItemVO cartItemVO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(cartController)
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        cartItemVO = CartItemVO.builder().cartItemId(1L).productId(1L)
                .productName("Headphones").sku("HP-001").quantity(2)
                .unitPrice(new BigDecimal("999.99")).totalPrice(new BigDecimal("1999.98")).build();

        cartVO = CartVO.builder().cartId(1L).userId(1L).items(List.of(cartItemVO))
                .subtotal(new BigDecimal("1999.98")).totalItems(2).build();
    }

    @Test
    void getCart_success() throws Exception {
        when(cartService.getCart(1L)).thenReturn(cartVO);
        mockMvc.perform(get("/api/cart/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    void getCart_notFound() throws Exception {
        when(cartService.getCart(1L)).thenThrow(new ResourceNotFoundException("Not found"));
        mockMvc.perform(get("/api/cart/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addItemToCart_success() throws Exception {
        when(cartService.addItemToCart(eq(1L), any())).thenReturn(cartVO);
        mockMvc.perform(post("/api/cart/1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartItemVO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    void updateCartItem_success() throws Exception {
        when(cartService.updateCartItem(1L, 1L, 5)).thenReturn(cartVO);
        mockMvc.perform(post("/api/cart/1/items/1").param("quantity", "5"))
                .andExpect(status().isOk());
    }

    @Test
    void removeItemFromCart_success() throws Exception {
        when(cartService.removeItemFromCart(1L, 1L)).thenReturn(cartVO);
        mockMvc.perform(post("/api/cart/1/items/1/remove"))
                .andExpect(status().isOk());
    }

    @Test
    void clearCart_success() throws Exception {
        when(cartService.clearCart(1L)).thenReturn(Map.of("status", "success", "message", "Cart cleared successfully"));
        mockMvc.perform(post("/api/cart/1/clear"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }
}
