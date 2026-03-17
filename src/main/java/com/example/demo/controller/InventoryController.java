package com.example.demo.controller;

import com.example.demo.service.InventoryService;
import com.example.demo.vo.inventory.InventoryReservationVO;
import com.example.demo.vo.inventory.InventoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryVO createInventory(@RequestBody InventoryVO inventoryVO) {
        return inventoryService.createInventory(inventoryVO);
    }

    @PostMapping("/{inventoryId}/restock")
    public InventoryVO restock(@PathVariable Long inventoryId, @RequestParam Integer quantity) {
        return inventoryService.restock(inventoryId, quantity);
    }

    @PostMapping("/{inventoryId}/reserve")
    public InventoryReservationVO reserve(@PathVariable Long inventoryId,
                                          @RequestParam Long orderId,
                                          @RequestParam Integer quantity) {
        return inventoryService.reserve(inventoryId, orderId, quantity);
    }

    @PostMapping("/release")
    public InventoryVO release(@RequestParam Long orderId, @RequestParam Long inventoryId) {
        return inventoryService.release(orderId, inventoryId);
    }

    @PostMapping("/confirm")
    public InventoryVO confirm(@RequestParam Long orderId, @RequestParam Long inventoryId) {
        return inventoryService.confirm(orderId, inventoryId);
    }
}
