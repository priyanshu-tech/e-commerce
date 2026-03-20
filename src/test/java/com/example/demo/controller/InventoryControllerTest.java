package com.example.demo.controller;

import com.example.demo.exception.GlobalExceptionHandler;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.service.InventoryService;
import com.example.demo.vo.inventory.InventoryReservationVO;
import com.example.demo.vo.inventory.InventoryVO;
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

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class InventoryControllerTest {

    @Mock InventoryService inventoryService;
    @InjectMocks InventoryController inventoryController;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();
    private InventoryVO inventoryVO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(inventoryController)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        inventoryVO = InventoryVO.builder().inventoryId(1L).productId(1L).sku("HP-001")
                .totalQuantity(100).availableQuantity(80).reservedQuantity(20)
                .minStockLevel(10).isLowStock(false).warehouseLocation("WH-A1").build();
    }

    @Test
    void getInventoryByProductId_success() throws Exception {
        when(inventoryService.getInventoryByProductId(1L)).thenReturn(inventoryVO);
        mockMvc.perform(get("/api/inventory/product/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("HP-001"));
    }

    @Test
    void getInventoryByProductId_notFound() throws Exception {
        when(inventoryService.getInventoryByProductId(1L)).thenThrow(new ResourceNotFoundException("Not found"));
        mockMvc.perform(get("/api/inventory/product/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getInventoryBySku_success() throws Exception {
        when(inventoryService.getInventoryBySku("HP-001")).thenReturn(inventoryVO);
        mockMvc.perform(get("/api/inventory/sku/HP-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1));
    }

    @Test
    void createInventory_success() throws Exception {
        when(inventoryService.createInventory(any())).thenReturn(inventoryVO);
        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inventoryVO)))
                .andExpect(status().isCreated());
    }

    @Test
    void restock_success() throws Exception {
        when(inventoryService.restock(1L, 50)).thenReturn(inventoryVO);
        mockMvc.perform(post("/api/inventory/1/restock").param("quantity", "50"))
                .andExpect(status().isOk());
    }

    @Test
    void reserve_success() throws Exception {
        InventoryReservationVO reservationVO = InventoryReservationVO.builder()
                .reservationId(1L).inventoryId(1L).orderId(1L).quantity(5)
                .expiresAt(LocalDateTime.now().plusMinutes(15)).build();
        when(inventoryService.reserve(1L, 1L, 5)).thenReturn(reservationVO);
        mockMvc.perform(post("/api/inventory/1/reserve")
                        .param("orderId", "1").param("quantity", "5"))
                .andExpect(status().isOk());
    }

    @Test
    void release_success() throws Exception {
        when(inventoryService.release(1L, 1L)).thenReturn(inventoryVO);
        mockMvc.perform(post("/api/inventory/release")
                        .param("orderId", "1").param("inventoryId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void confirm_success() throws Exception {
        when(inventoryService.confirm(1L, 1L)).thenReturn(inventoryVO);
        mockMvc.perform(post("/api/inventory/confirm")
                        .param("orderId", "1").param("inventoryId", "1"))
                .andExpect(status().isOk());
    }
}
