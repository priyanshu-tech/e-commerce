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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
@Tag("integration")
class CartIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private long createUser(String username, String email) throws Exception {
        String res = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "email", email,
                                "firstName", "Cart",
                                "lastName", "User"
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(res).get("userId").asLong();
    }

    private long createProductWithInventory(String sku, int qty) throws Exception {
        String res = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Cart Product " + sku,
                                "sku", sku,
                                "price", 1000,
                                "brand", "Brand",
                                "categoryId", 1
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long productId = objectMapper.readTree(res).get("productId").asLong();

        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sku", sku,
                                "totalQuantity", qty,
                                "availableQuantity", qty,
                                "reservedQuantity", 0,
                                "minStockLevel", 5
                        ))))
                .andExpect(status().isCreated());

        return productId;
    }

    // ── Happy Journeys ──────────────────────────────────────────────────────

    @Test
    void getCart_autoCreated_success() throws Exception {
        long userId = createUser("cart_auto_user", "cartauto@example.com");

        mockMvc.perform(get("/api/cart/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void addItemToCart_success() throws Exception {
        long userId = createUser("cart_add_user", "cartadd@example.com");
        long productId = createProductWithInventory("CART-ADD-001", 50);

        mockMvc.perform(post("/api/cart/{userId}/items", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", productId,
                                "quantity", 2
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    void addItemToCart_duplicate_incrementsQuantity() throws Exception {
        long userId = createUser("cart_dup_user", "cartdup@example.com");
        long productId = createProductWithInventory("CART-DUP-001", 50);

        mockMvc.perform(post("/api/cart/{userId}/items", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("productId", productId, "quantity", 2))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/cart/{userId}/items", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("productId", productId, "quantity", 3))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].quantity").value(5))
                .andExpect(jsonPath("$.totalItems").value(5));
    }

    @Test
    void updateCartItem_success() throws Exception {
        long userId = createUser("cart_update_user", "cartupdate@example.com");
        long productId = createProductWithInventory("CART-UPD-001", 50);

        String res = mockMvc.perform(post("/api/cart/{userId}/items", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("productId", productId, "quantity", 1))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long cartItemId = objectMapper.readTree(res).get("items").get(0).get("cartItemId").asLong();

        mockMvc.perform(post("/api/cart/{userId}/items/{cartItemId}", userId, cartItemId)
                        .param("quantity", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(10));
    }

    @Test
    void removeItemFromCart_success() throws Exception {
        long userId = createUser("cart_remove_user", "cartremove@example.com");
        long productId = createProductWithInventory("CART-REM-001", 50);

        String res = mockMvc.perform(post("/api/cart/{userId}/items", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("productId", productId, "quantity", 1))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long cartItemId = objectMapper.readTree(res).get("items").get(0).get("cartItemId").asLong();

        mockMvc.perform(post("/api/cart/{userId}/items/{cartItemId}/remove", userId, cartItemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void clearCart_success() throws Exception {
        long userId = createUser("cart_clear_user", "cartclear@example.com");
        long productId = createProductWithInventory("CART-CLR-001", 50);

        mockMvc.perform(post("/api/cart/{userId}/items", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("productId", productId, "quantity", 2))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/cart/{userId}/clear", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        mockMvc.perform(get("/api/cart/{userId}", userId))
                .andExpect(jsonPath("$.items").isEmpty());
    }

    // ── Failure Journeys ────────────────────────────────────────────────────

    @Test
    void addItemToCart_productNotFound_returns404() throws Exception {
        long userId = createUser("cart_noprod_user", "cartnoprod@example.com");

        mockMvc.perform(post("/api/cart/{userId}/items", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("productId", 99999L, "quantity", 1))))
                .andExpect(status().isNotFound());
    }

    @Test
    void addItemToCart_inactiveProduct_returns500() throws Exception {
        long userId = createUser("cart_inactive_user", "cartinactive@example.com");
        long productId = createProductWithInventory("CART-INACTIVE-001", 50);

        // toggle to INACTIVE
        mockMvc.perform(post("/api/products/{productId}/toggle-status", productId))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/cart/{userId}/items", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("productId", productId, "quantity", 1))))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void addItemToCart_outOfStock_returns500() throws Exception {
        long userId = createUser("cart_oos_user", "cartoos@example.com");
        long productId = createProductWithInventory("CART-OOS-001", 0);

        mockMvc.perform(post("/api/cart/{userId}/items", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("productId", productId, "quantity", 1))))
                .andExpect(status().isInternalServerError());
    }
}
