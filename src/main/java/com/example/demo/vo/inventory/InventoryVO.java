package com.example.demo.vo.inventory;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Inventory Value Object
 * Represents product inventory/stock information
 */
@Data
@Builder
public class InventoryVO {
    private Long inventoryId;
    private Long productId;
    private String sku;
    private Integer availableQuantity;
    private Integer reservedQuantity;
    private String warehouseLocation;
    private LocalDateTime lastUpdated;
}
