package com.example.demo.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
@Tag("integration")
class InventoryIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    /** Creates a product and returns its productId + sku */
    private long[] createProduct(String sku) throws Exception {
        String res = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Inv Product " + sku,
                                "sku", sku,
                                "price", 500,
                                "brand", "Brand",
                                "categoryId", 1
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long productId = objectMapper.readTree(res).get("productId").asLong();
        return new long[]{productId};
    }

    private long createInventory(String sku, int qty) throws Exception {
        String res = mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sku", sku,
                                "totalQuantity", qty,
                                "availableQuantity", qty,
                                "reservedQuantity", 0,
                                "minStockLevel", 5,
                                "warehouseLocation", "WH-A"
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(res).get("inventoryId").asLong();
    }

    // ── Happy Journeys ──────────────────────────────────────────────────────

    @Test
    void createInventory_success() throws Exception {
        createProduct("INV-CREATE-001");
        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sku", "INV-CREATE-001",
                                "totalQuantity", 100,
                                "availableQuantity", 100,
                                "reservedQuantity", 0,
                                "minStockLevel", 10,
                                "warehouseLocation", "WH-A"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inventoryId").isNumber())
                .andExpect(jsonPath("$.availableQuantity").value(100))
                .andExpect(jsonPath("$.isLowStock").value(false));
    }

    @Test
    void getInventoryByProductId_success() throws Exception {
        long[] ids = createProduct("INV-BYPROD-001");
        createInventory("INV-BYPROD-001", 50);

        mockMvc.perform(get("/api/inventory/product/{productId}", ids[0]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("INV-BYPROD-001"))
                .andExpect(jsonPath("$.availableQuantity").value(50));
    }

    @Test
    void getInventoryBySku_success() throws Exception {
        createProduct("INV-BYSKU-001");
        createInventory("INV-BYSKU-001", 30);

        mockMvc.perform(get("/api/inventory/sku/{sku}", "INV-BYSKU-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(30));
    }

    @Test
    void restock_success() throws Exception {
        createProduct("INV-RESTOCK-001");
        long inventoryId = createInventory("INV-RESTOCK-001", 10);

        mockMvc.perform(post("/api/inventory/{inventoryId}/restock", inventoryId)
                        .param("quantity", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuantity").value(60))
                .andExpect(jsonPath("$.availableQuantity").value(60));
    }

    @Test
    void reserve_and_release_success() throws Exception {
        createProduct("INV-RESERVE-001");
        long inventoryId = createInventory("INV-RESERVE-001", 20);

        // reserve
        String res = mockMvc.perform(post("/api/inventory/{inventoryId}/reserve", inventoryId)
                        .param("orderId", "9001")
                        .param("quantity", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(5))
                .andReturn().getResponse().getContentAsString();

        // verify available decreased
        mockMvc.perform(get("/api/inventory/sku/{sku}", "INV-RESERVE-001"))
                .andExpect(jsonPath("$.availableQuantity").value(15))
                .andExpect(jsonPath("$.reservedQuantity").value(5));

        // release
        mockMvc.perform(post("/api/inventory/release")
                        .param("orderId", "9001")
                        .param("inventoryId", String.valueOf(inventoryId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(20))
                .andExpect(jsonPath("$.reservedQuantity").value(0));
    }

    @Test
    void reserve_and_confirm_success() throws Exception {
        createProduct("INV-CONFIRM-001");
        long inventoryId = createInventory("INV-CONFIRM-001", 20);

        mockMvc.perform(post("/api/inventory/{inventoryId}/reserve", inventoryId)
                        .param("orderId", "9002")
                        .param("quantity", "3"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/inventory/confirm")
                        .param("orderId", "9002")
                        .param("inventoryId", String.valueOf(inventoryId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservedQuantity").value(0))
                .andExpect(jsonPath("$.totalQuantity").value(17));
    }

    @Test
    void isLowStock_true_whenBelowMinLevel() throws Exception {
        createProduct("INV-LOWSTOCK-001");
        long inventoryId = createInventory("INV-LOWSTOCK-001", 3);

        // minStockLevel is 5, available is 3 → isLowStock = true
        mockMvc.perform(get("/api/inventory/sku/{sku}", "INV-LOWSTOCK-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isLowStock").value(true));
    }

    // ── Failure Journeys ────────────────────────────────────────────────────

    @Test
    void createInventory_skuNotFound_returns404() throws Exception {
        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sku", "GHOST-SKU-999",
                                "totalQuantity", 10,
                                "availableQuantity", 10,
                                "reservedQuantity", 0,
                                "minStockLevel", 5
                        ))))
                .andExpect(status().isNotFound());
    }

    @Test
    void reserve_insufficientStock_returns500() throws Exception {
        createProduct("INV-OVERRESERVE-001");
        long inventoryId = createInventory("INV-OVERRESERVE-001", 2);

        mockMvc.perform(post("/api/inventory/{inventoryId}/reserve", inventoryId)
                        .param("orderId", "9003")
                        .param("quantity", "10"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getInventoryByProductId_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/inventory/product/{productId}", 99999L))
                .andExpect(status().isNotFound());
    }
}
