package com.example.demo.service.impl;

import com.example.demo.entity.Inventory;
import com.example.demo.entity.InventoryReservation;
import com.example.demo.util.LogUtils;
import com.example.demo.vo.inventory.InventoryReservationVO;
import com.example.demo.vo.inventory.InventoryVO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class InventoryMapper {

    static InventoryVO toVO(Inventory inventory) {
        InventoryVO result = InventoryVO.builder()
                .inventoryId(inventory.getInventoryId())
                .productId(inventory.getProductId())
                .sku(inventory.getSku())
                .totalQuantity(inventory.getTotalQuantity())
                .availableQuantity(inventory.getAvailableQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .minStockLevel(inventory.getMinStockLevel())
                .isLowStock(inventory.getAvailableQuantity() <= inventory.getMinStockLevel())
                .warehouseLocation(inventory.getWarehouseLocation())
                .lastUpdated(inventory.getLastUpdated())
                .build();
        LogUtils.info(log, "Mapped Inventory to InventoryVO", result);
        return result;
    }

    static InventoryReservationVO toReservationVO(InventoryReservation reservation) {
        InventoryReservationVO result = InventoryReservationVO.builder()
                .reservationId(reservation.getReservationId())
                .inventoryId(reservation.getInventoryId())
                .orderId(reservation.getOrderId())
                .quantity(reservation.getQuantity())
                .expiresAt(reservation.getExpiresAt())
                .createdAt(reservation.getCreatedAt())
                .build();
        LogUtils.info(log, "Mapped InventoryReservation to VO", result);
        return result;
    }
}
