package com.example.demo.service;

import com.example.demo.vo.inventory.InventoryVO;

/**
 * Inventory Service Interface
 * Handles inventory and stock management business logic
 */
public interface InventoryService {

    InventoryVO getInventoryByProductId(Long productId);

    InventoryVO getInventoryBySku(String sku);

    InventoryVO updateInventory(Long inventoryId, InventoryVO inventoryVO);

    InventoryVO reserveInventory(Long inventoryId, Integer quantity);

    InventoryVO releaseInventory(Long inventoryId, Integer quantity);
}
