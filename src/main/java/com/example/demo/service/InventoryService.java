package com.example.demo.service;

import com.example.demo.vo.inventory.InventoryReservationVO;
import com.example.demo.vo.inventory.InventoryVO;

public interface InventoryService {

    InventoryVO getInventoryByProductId(Long productId);

    InventoryVO getInventoryBySku(String sku);

    InventoryVO createInventory(InventoryVO inventoryVO);

    InventoryVO restock(Long inventoryId, Integer quantity);

    InventoryReservationVO reserve(Long inventoryId, Long orderId, Integer quantity);

    InventoryVO release(Long orderId, Long inventoryId);

    InventoryVO confirm(Long orderId, Long inventoryId);
}
