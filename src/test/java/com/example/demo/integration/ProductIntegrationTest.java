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
class ProductIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    /** Creates a category and returns its categoryId */
    private long createCategory(String name, int displayOrder) throws Exception {
        // Categories are created via CategoryRepository directly — use a helper product to seed,
        // or POST to a category endpoint if it exists. Since there's no category endpoint,
        // we insert via a product that creates the category implicitly.
        // Actually ProductMapper.toEntity uses vo.getCategoryId() — category must pre-exist.
        // So we need to insert category via repository. Instead, use the fact that
        // ProductServiceImpl.createProduct calls productRepository.save(ProductMapper.toEntity(productVO))
        // which uses categoryId directly — category does NOT need to exist (no FK in H2 schema).
        // So we just need a valid Long categoryId. Use a fixed seed value per test.
        return displayOrder; // use displayOrder as a simple unique categoryId seed
    }

    /** Creates a product with a given categoryId and returns productId */
    private long createProduct(String name, String sku, long categoryId) throws Exception {
        String res = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name,
                                "sku", sku,
                                "price", 1000,
                                "brand", "Brand",
                                "categoryId", categoryId
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(res).get("productId").asLong();
    }

    // ── Happy Journeys ──────────────────────────────────────────────────────

    @Test
    void createProduct_success() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Test Laptop",
                                "description", "A great laptop",
                                "sku", "LAPTOP-001",
                                "price", 75000,
                                "brand", "Dell",
                                "categoryId", 1
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").isNumber())
                .andExpect(jsonPath("$.sku").value("LAPTOP-001"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getProductById_success() throws Exception {
        long productId = createProduct("Get Product Test", "GET-PROD-001", 1L);

        mockMvc.perform(get("/api/products/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.sku").value("GET-PROD-001"));
    }

    @Test
    void getAllProducts_noFilter_returnsActiveProducts() throws Exception {
        createProduct("Listed Product", "LIST-001", 1L);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.sku == 'LIST-001')]", hasSize(1)));
    }

    @Test
    void toggleStatus_activeToInactive_andBack() throws Exception {
        long productId = createProduct("Toggle Product", "TOGGLE-001", 1L);

        mockMvc.perform(post("/api/products/{productId}/toggle-status", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("INACTIVE")));

        mockMvc.perform(post("/api/products/{productId}/toggle-status", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("ACTIVE")));
    }

    @Test
    void updateRating_success() throws Exception {
        long productId = createProduct("Rating Product", "RATING-001", 1L);

        mockMvc.perform(post("/api/products/{productId}/rating", productId)
                        .param("rating", "4.5")
                        .param("reviewCount", "120"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(4.5))
                .andExpect(jsonPath("$.reviewCount").value(120));
    }

    @Test
    void updateProduct_success() throws Exception {
        long productId = createProduct("Old Name", "UPDATE-PROD-001", 1L);

        mockMvc.perform(post("/api/products/{productId}/update", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "New Name",
                                "sku", "UPDATE-PROD-001",
                                "price", 2000,
                                "brand", "NewBrand",
                                "categoryId", 1
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.brand").value("NewBrand"));
    }

    // ── Failure Journeys ────────────────────────────────────────────────────

    @Test
    void createProduct_duplicateSku_returns409() throws Exception {
        createProduct("Dup SKU Product 1", "DUP-SKU-001", 1L);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Dup SKU Product 2",
                                "sku", "DUP-SKU-001",
                                "price", 200,
                                "brand", "Y",
                                "categoryId", 1
                        ))))
                .andExpect(status().isConflict());
    }

    @Test
    void getProductById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/products/{productId}", 99999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void toggleStatus_productNotFound_returns404() throws Exception {
        mockMvc.perform(post("/api/products/{productId}/toggle-status", 99999L))
                .andExpect(status().isNotFound());
    }
}
