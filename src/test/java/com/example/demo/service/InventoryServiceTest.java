package com.example.demo.service;

import com.example.demo.entity.Inventory;
import com.example.demo.entity.InventoryReservation;
import com.example.demo.entity.Product;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.InventoryRepository;
import com.example.demo.repository.InventoryReservationRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.service.impl.InventoryServiceImpl;
import com.example.demo.vo.inventory.InventoryReservationVO;
import com.example.demo.vo.inventory.InventoryVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock InventoryRepository inventoryRepository;
    @Mock InventoryReservationRepository reservationRepository;
    @Mock ProductRepository productRepository;
    @InjectMocks InventoryServiceImpl inventoryService;

    private Inventory inventory;
    private InventoryReservation reservation;

    @BeforeEach
    void setUp() {
        inventory = Inventory.builder()
                .inventoryId(1L).productId(1L).sku("HP-001")
                .totalQuantity(100).availableQuantity(80)
                .reservedQuantity(20).minStockLevel(10)
                .warehouseLocation("WH-A1").lastUpdated(LocalDateTime.now()).build();

        reservation = InventoryReservation.builder()
                .reservationId(1L).inventoryId(1L).orderId(1L)
                .quantity(5).expiresAt(LocalDateTime.now().plusMinutes(15))
                .createdAt(LocalDateTime.now()).build();
    }

    @Test
    void getInventoryByProductId_success() {
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
        InventoryVO result = inventoryService.getInventoryByProductId(1L);
        assertThat(result.getSku()).isEqualTo("HP-001");
        assertThat(result.getIsLowStock()).isFalse();
    }

    @Test
    void getInventoryByProductId_notFound() {
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> inventoryService.getInventoryByProductId(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getInventoryBySku_success() {
        when(inventoryRepository.findBySku("HP-001")).thenReturn(Optional.of(inventory));
        InventoryVO result = inventoryService.getInventoryBySku("HP-001");
        assertThat(result.getProductId()).isEqualTo(1L);
    }

    @Test
    void getInventoryBySku_notFound() {
        when(inventoryRepository.findBySku("UNKNOWN")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> inventoryService.getInventoryBySku("UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createInventory_success() {
        Product product = Product.builder().productId(1L).sku("HP-001").build();
        when(productRepository.findBySku("HP-001")).thenReturn(Optional.of(product));
        when(inventoryRepository.existsByProductId(1L)).thenReturn(false);
        when(inventoryRepository.save(any())).thenReturn(inventory);

        InventoryVO vo = InventoryVO.builder().sku("HP-001").totalQuantity(100).minStockLevel(10).build();
        InventoryVO result = inventoryService.createInventory(vo);
        assertThat(result.getSku()).isEqualTo("HP-001");
    }

    @Test
    void createInventory_productNotFound() {
        when(productRepository.findBySku("UNKNOWN")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> inventoryService.createInventory(InventoryVO.builder().sku("UNKNOWN").build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createInventory_duplicate() {
        Product product = Product.builder().productId(1L).sku("HP-001").build();
        when(productRepository.findBySku("HP-001")).thenReturn(Optional.of(product));
        when(inventoryRepository.existsByProductId(1L)).thenReturn(true);
        assertThatThrownBy(() -> inventoryService.createInventory(InventoryVO.builder().sku("HP-001").build()))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void createInventory_nullQuantities() {
        Product product = Product.builder().productId(1L).sku("HP-001").build();
        when(productRepository.findBySku("HP-001")).thenReturn(Optional.of(product));
        when(inventoryRepository.existsByProductId(1L)).thenReturn(false);
        when(inventoryRepository.save(any())).thenReturn(inventory);

        InventoryVO vo = InventoryVO.builder().sku("HP-001").build();
        InventoryVO result = inventoryService.createInventory(vo);
        assertThat(result).isNotNull();
    }

    @Test
    void restock_success() {
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any())).thenReturn(inventory);
        InventoryVO result = inventoryService.restock(1L, 50);
        assertThat(result).isNotNull();
    }

    @Test
    void restock_notFound() {
        when(inventoryRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> inventoryService.restock(1L, 50))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void reserve_success() {
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any())).thenReturn(inventory);
        when(reservationRepository.save(any())).thenReturn(reservation);
        InventoryReservationVO result = inventoryService.reserve(1L, 1L, 5);
        assertThat(result.getQuantity()).isEqualTo(5);
    }

    @Test
    void reserve_insufficientStock() {
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        assertThatThrownBy(() -> inventoryService.reserve(1L, 1L, 999))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void reserve_notFound() {
        when(inventoryRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> inventoryService.reserve(1L, 1L, 5))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void release_success() {
        when(reservationRepository.findByOrderIdAndInventoryId(1L, 1L)).thenReturn(Optional.of(reservation));
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any())).thenReturn(inventory);
        InventoryVO result = inventoryService.release(1L, 1L);
        assertThat(result).isNotNull();
    }

    @Test
    void release_reservationNotFound() {
        when(reservationRepository.findByOrderIdAndInventoryId(1L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> inventoryService.release(1L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void confirm_success() {
        when(reservationRepository.findByOrderIdAndInventoryId(1L, 1L)).thenReturn(Optional.of(reservation));
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any())).thenReturn(inventory);
        InventoryVO result = inventoryService.confirm(1L, 1L);
        assertThat(result).isNotNull();
    }

    @Test
    void confirm_reservationNotFound() {
        when(reservationRepository.findByOrderIdAndInventoryId(1L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> inventoryService.confirm(1L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void releaseExpiredReservations_withExpired() {
        when(reservationRepository.findExpiredReservations(any())).thenReturn(List.of(reservation));
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any())).thenReturn(inventory);
        inventoryService.releaseExpiredReservations();
        verify(reservationRepository).delete(reservation);
    }

    @Test
    void releaseExpiredReservations_noExpired() {
        when(reservationRepository.findExpiredReservations(any())).thenReturn(List.of());
        inventoryService.releaseExpiredReservations();
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void getInventoryByProductId_lowStock() {
        inventory.setAvailableQuantity(5);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
        InventoryVO result = inventoryService.getInventoryByProductId(1L);
        assertThat(result.getIsLowStock()).isTrue();
    }
}
