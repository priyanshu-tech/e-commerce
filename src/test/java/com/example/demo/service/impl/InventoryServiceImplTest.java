package com.example.demo.service.impl;

import com.example.demo.entity.Inventory;
import com.example.demo.entity.InventoryReservation;
import com.example.demo.entity.Product;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.InventoryRepository;
import com.example.demo.repository.InventoryReservationRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.vo.inventory.InventoryVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock InventoryRepository inventoryRepository;
    @Mock InventoryReservationRepository reservationRepository;
    @Mock ProductRepository productRepository;
    @InjectMocks InventoryServiceImpl inventoryService;

    private Inventory buildInventory(int available, int reserved) {
        return Inventory.builder()
                .inventoryId(1L).productId(1L).sku("SKU-001")
                .totalQuantity(available + reserved)
                .availableQuantity(available)
                .reservedQuantity(reserved)
                .minStockLevel(10).build();
    }

    // ── createInventory ──────────────────────────────────────

    @Test
    void createInventory_success() {
        InventoryVO vo = InventoryVO.builder().sku("SKU-001").totalQuantity(100).minStockLevel(10).build();
        Product product = Product.builder().productId(1L).sku("SKU-001").build();
        Inventory saved = buildInventory(100, 0);

        when(productRepository.findBySku("SKU-001")).thenReturn(Optional.of(product));
        when(inventoryRepository.existsByProductId(1L)).thenReturn(false);
        when(inventoryRepository.save(any())).thenReturn(saved);

        InventoryVO result = inventoryService.createInventory(vo);

        assertThat(result.getAvailableQuantity()).isEqualTo(100);
    }

    @Test
    void createInventory_productNotFound_throws() {
        InventoryVO vo = InventoryVO.builder().sku("INVALID").build();
        when(productRepository.findBySku("INVALID")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.createInventory(vo))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createInventory_duplicate_throws() {
        InventoryVO vo = InventoryVO.builder().sku("SKU-001").build();
        Product product = Product.builder().productId(1L).sku("SKU-001").build();

        when(productRepository.findBySku("SKU-001")).thenReturn(Optional.of(product));
        when(inventoryRepository.existsByProductId(1L)).thenReturn(true);

        assertThatThrownBy(() -> inventoryService.createInventory(vo))
                .isInstanceOf(DuplicateResourceException.class);
    }

    // ── restock ──────────────────────────────────────────────

    @Test
    void restock_success() {
        Inventory inventory = buildInventory(50, 0);
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any())).thenReturn(inventory);

        InventoryVO result = inventoryService.restock(1L, 20);

        assertThat(result.getAvailableQuantity()).isEqualTo(70);
        assertThat(result.getTotalQuantity()).isEqualTo(70);
    }

    @Test
    void restock_notFound_throws() {
        when(inventoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.restock(99L, 10))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── reserve ──────────────────────────────────────────────

    @Test
    void reserve_success() {
        Inventory inventory = buildInventory(50, 0);
        InventoryReservation saved = InventoryReservation.builder()
                .reservationId(1L).inventoryId(1L).orderId(10L).quantity(5).build();

        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any())).thenReturn(inventory);
        when(reservationRepository.save(any())).thenReturn(saved);

        var result = inventoryService.reserve(1L, 10L, 5);

        assertThat(result.getQuantity()).isEqualTo(5);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(45);
        assertThat(inventory.getReservedQuantity()).isEqualTo(5);
    }

    @Test
    void reserve_insufficientStock_throws() {
        Inventory inventory = buildInventory(3, 0);
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.reserve(1L, 10L, 5))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient stock");
    }

    // ── release ──────────────────────────────────────────────

    @Test
    void release_success() {
        Inventory inventory = buildInventory(45, 5);
        InventoryReservation reservation = InventoryReservation.builder()
                .reservationId(1L).inventoryId(1L).orderId(10L).quantity(5).build();

        when(reservationRepository.findByOrderIdAndInventoryId(10L, 1L)).thenReturn(Optional.of(reservation));
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any())).thenReturn(inventory);

        InventoryVO result = inventoryService.release(10L, 1L);

        assertThat(inventory.getAvailableQuantity()).isEqualTo(50);
        assertThat(inventory.getReservedQuantity()).isEqualTo(0);
        verify(reservationRepository).delete(reservation);
    }

    @Test
    void release_reservationNotFound_throws() {
        when(reservationRepository.findByOrderIdAndInventoryId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.release(99L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
