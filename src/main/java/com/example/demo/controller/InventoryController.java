package com.example.demo.controller;

import com.example.demo.service.InventoryService;
import com.example.demo.vo.inventory.InventoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Inventory Controller
 * Handles inventory and stock management
 */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    @Autowired
    private final InventoryService inventoryService;

    @GetMapping("/product/{productId}")
    public InventoryVO getInventoryByProductId(@PathVariable Long productId) {
        return inventoryService.getInventoryByProductId(productId);
    }

    @GetMapping("/sku/{sku}")
    public InventoryVO getInventoryBySku(@PathVariable String sku) {
        return inventoryService.getInventoryBySku(sku);
    }

    @PostMapping("/{inventoryId}/update")
    public InventoryVO updateInventory(
            @PathVariable Long inventoryId,
            @RequestBody InventoryVO inventoryVO) {
        return inventoryService.updateInventory(inventoryId, inventoryVO);
    }

    @PostMapping("/{inventoryId}/reserve")
    public InventoryVO reserveInventory(
            @PathVariable Long inventoryId,
            @RequestParam Integer quantity) {
        return inventoryService.reserveInventory(inventoryId, quantity);
    }

    @PostMapping("/{inventoryId}/release")
    public InventoryVO releaseInventory(
            @PathVariable Long inventoryId,
            @RequestParam Integer quantity) {
        return inventoryService.releaseInventory(inventoryId, quantity);
    }
}
