package com.example.demo.service.impl;

import com.example.demo.entity.Inventory;
import com.example.demo.entity.InventoryReservation;
import com.example.demo.entity.Product;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.InventoryRepository;
import com.example.demo.repository.InventoryReservationRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.service.InventoryService;
import com.example.demo.util.LogUtils;
import com.example.demo.vo.inventory.InventoryReservationVO;
import com.example.demo.vo.inventory.InventoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private static final int RESERVATION_EXPIRY_MINUTES = 15;

    @Autowired private final InventoryRepository inventoryRepository;
    @Autowired private final InventoryReservationRepository reservationRepository;
    @Autowired private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public InventoryVO getInventoryByProductId(Long productId) {
        log.info("Getting inventory for productId: {}", productId);
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for productId: " + productId));
        InventoryVO result = InventoryMapper.toVO(inventory);
        LogUtils.info(log, "Fetched inventory by productId", result);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryVO getInventoryBySku(String sku) {
        log.info("Getting inventory for sku: {}", sku);
        Inventory inventory = inventoryRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for SKU: " + sku));
        InventoryVO result = InventoryMapper.toVO(inventory);
        LogUtils.info(log, "Fetched inventory by sku", result);
        return result;
    }

    @Override
    @Transactional
    public InventoryVO createInventory(InventoryVO inventoryVO) {
        LogUtils.info(log, "Creating inventory", inventoryVO);
        Product product = productRepository.findBySku(inventoryVO.getSku())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with SKU: " + inventoryVO.getSku()));
        if (inventoryRepository.existsByProductId(product.getProductId())) {
            throw new DuplicateResourceException("Inventory already exists for SKU: " + inventoryVO.getSku());
        }
        Inventory inventory = Inventory.builder()
                .productId(product.getProductId())
                .sku(inventoryVO.getSku())
                .totalQuantity(inventoryVO.getTotalQuantity() != null ? inventoryVO.getTotalQuantity() : 0)
                .availableQuantity(inventoryVO.getTotalQuantity() != null ? inventoryVO.getTotalQuantity() : 0)
                .reservedQuantity(0)
                .minStockLevel(inventoryVO.getMinStockLevel() != null ? inventoryVO.getMinStockLevel() : 10)
                .warehouseLocation(inventoryVO.getWarehouseLocation())
                .build();
        Inventory saved = inventoryRepository.save(inventory);
        InventoryVO result = InventoryMapper.toVO(saved);
        LogUtils.info(log, "Inventory created", result);
        return result;
    }

    @Override
    @Transactional
    public InventoryVO restock(Long inventoryId, Integer quantity) {
        log.info("Restocking inventoryId: {} with quantity: {}", inventoryId, quantity);
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found with id: " + inventoryId));
        inventory.setTotalQuantity(inventory.getTotalQuantity() + quantity);
        inventory.setAvailableQuantity(inventory.getAvailableQuantity() + quantity);
        Inventory updated = inventoryRepository.save(inventory);
        InventoryVO result = InventoryMapper.toVO(updated);
        LogUtils.info(log, "Inventory restocked", result);
        return result;
    }

    @Override
    @Transactional
    public InventoryReservationVO reserve(Long inventoryId, Long orderId, Integer quantity) {
        log.info("Reserving {} units for inventoryId: {}, orderId: {}", quantity, inventoryId, orderId);
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found with id: " + inventoryId));

        if (inventory.getAvailableQuantity() < quantity) {
            throw new IllegalStateException("Insufficient stock. Available: " + inventory.getAvailableQuantity() + ", Requested: " + quantity);
        }

        inventory.setAvailableQuantity(inventory.getAvailableQuantity() - quantity);
        inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
        inventoryRepository.save(inventory);

        InventoryReservation reservation = InventoryReservation.builder()
                .inventoryId(inventoryId)
                .orderId(orderId)
                .quantity(quantity)
                .expiresAt(LocalDateTime.now().plusMinutes(RESERVATION_EXPIRY_MINUTES))
                .build();
        InventoryReservation saved = reservationRepository.save(reservation);
        InventoryReservationVO result = InventoryMapper.toReservationVO(saved);
        LogUtils.info(log, "Inventory reserved", result);
        return result;
    }

    @Override
    @Transactional
    public InventoryVO release(Long orderId, Long inventoryId) {
        log.info("Releasing reservation for orderId: {}, inventoryId: {}", orderId, inventoryId);
        InventoryReservation reservation = reservationRepository.findByOrderIdAndInventoryId(orderId, inventoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found for orderId: " + orderId + " and inventoryId: " + inventoryId));

        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found with id: " + inventoryId));

        inventory.setAvailableQuantity(inventory.getAvailableQuantity() + reservation.getQuantity());
        inventory.setReservedQuantity(inventory.getReservedQuantity() - reservation.getQuantity());
        inventoryRepository.save(inventory);
        reservationRepository.delete(reservation);

        InventoryVO result = InventoryMapper.toVO(inventory);
        LogUtils.info(log, "Inventory released", result);
        return result;
    }

    @Override
    @Transactional
    public InventoryVO confirm(Long orderId, Long inventoryId) {
        log.info("Confirming reservation for orderId: {}, inventoryId: {}", orderId, inventoryId);
        InventoryReservation reservation = reservationRepository.findByOrderIdAndInventoryId(orderId, inventoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found for orderId: " + orderId + " and inventoryId: " + inventoryId));

        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found with id: " + inventoryId));

        inventory.setTotalQuantity(inventory.getTotalQuantity() - reservation.getQuantity());
        inventory.setReservedQuantity(inventory.getReservedQuantity() - reservation.getQuantity());
        inventoryRepository.save(inventory);
        reservationRepository.delete(reservation);

        InventoryVO result = InventoryMapper.toVO(inventory);
        LogUtils.info(log, "Inventory confirmed", result);
        return result;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void releaseExpiredReservations() {
        List<InventoryReservation> expired = reservationRepository.findExpiredReservations(LocalDateTime.now());
        if (expired.isEmpty()) return;

        log.info("Found {} expired reservations — releasing", expired.size());
        expired.forEach(reservation -> {
            inventoryRepository.findById(reservation.getInventoryId()).ifPresent(inventory -> {
                inventory.setAvailableQuantity(inventory.getAvailableQuantity() + reservation.getQuantity());
                inventory.setReservedQuantity(inventory.getReservedQuantity() - reservation.getQuantity());
                inventoryRepository.save(inventory);
                log.info("Released expired reservation {} for inventoryId: {}", reservation.getReservationId(), reservation.getInventoryId());
            });
            reservationRepository.delete(reservation);
        });
    }
}
