package com.example.demo.service.impl;

import com.example.demo.service.InventoryService;
import com.example.demo.util.LogUtils;
import com.example.demo.vo.inventory.InventoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    @Override
    public InventoryVO getInventoryByProductId(Long productId) {
        log.info("Getting inventory for productId: {}", productId);
        InventoryVO result = InventoryVO.builder().productId(productId).build();
        LogUtils.info(log, "Fetched inventory by productId", result);
        return result;
    }

    @Override
    public InventoryVO getInventoryBySku(String sku) {
        log.info("Getting inventory for sku: {}", sku);
        InventoryVO result = InventoryVO.builder().sku(sku).build();
        LogUtils.info(log, "Fetched inventory by sku", result);
        return result;
    }

    @Override
    public InventoryVO updateInventory(Long inventoryId, InventoryVO inventoryVO) {
        log.info("Updating inventoryId: {}", inventoryId);
        LogUtils.info(log, "Update payload", inventoryVO);
        LogUtils.info(log, "Inventory updated", inventoryVO);
        return inventoryVO;
    }

    @Override
    public InventoryVO reserveInventory(Long inventoryId, Integer quantity) {
        log.info("Reserving {} units for inventoryId: {}", quantity, inventoryId);
        InventoryVO result = InventoryVO.builder().inventoryId(inventoryId).build();
        LogUtils.info(log, "Inventory reserved", result);
        return result;
    }

    @Override
    public InventoryVO releaseInventory(Long inventoryId, Integer quantity) {
        log.info("Releasing {} units for inventoryId: {}", quantity, inventoryId);
        InventoryVO result = InventoryVO.builder().inventoryId(inventoryId).build();
        LogUtils.info(log, "Inventory released", result);
        return result;
    }
}
