package com.example.demo.vo.inventory;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InventoryVO {
    private Long inventoryId;
    private Long productId;
    private String sku;
    private Integer totalQuantity;
    private Integer availableQuantity;
    private Integer reservedQuantity;
    private Integer minStockLevel;
    private Boolean isLowStock;
    private String warehouseLocation;
    private LocalDateTime lastUpdated;
}
