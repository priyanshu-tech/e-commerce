package com.example.demo.controller;

import com.example.demo.exception.GlobalExceptionHandler;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.service.ProductService;
import com.example.demo.vo.product.CategoryVO;
import com.example.demo.vo.product.ProductVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock ProductService productService;
    @InjectMocks ProductController productController;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    private ProductVO productVO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        productVO = ProductVO.builder().productId(1L).name("Headphones").sku("HP-001")
                .price(new BigDecimal("999.99")).brand("Sony").categoryId(1L).status("ACTIVE").build();
    }

    @Test
    void getAllProducts_success() throws Exception {
        when(productService.getAllProducts(any(), any(), anyInt(), anyInt())).thenReturn(List.of(productVO));
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sku").value("HP-001"));
    }

    @Test
    void getAllProducts_withFilters() throws Exception {
        when(productService.getAllProducts(eq("Electronics"), eq("head"), anyInt(), anyInt()))
                .thenReturn(List.of(productVO));
        mockMvc.perform(get("/api/products")
                        .param("category", "Electronics").param("search", "head")
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void getProductById_success() throws Exception {
        when(productService.getProductById(1L)).thenReturn(productVO);
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("HP-001"));
    }

    @Test
    void getProductById_notFound() throws Exception {
        when(productService.getProductById(1L)).thenThrow(new ResourceNotFoundException("Not found"));
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createProduct_success() throws Exception {
        when(productService.createProduct(any())).thenReturn(productVO);
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productVO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("HP-001"));
    }

    @Test
    void updateProduct_success() throws Exception {
        when(productService.updateProduct(eq(1L), any())).thenReturn(productVO);
        mockMvc.perform(post("/api/products/1/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productVO)))
                .andExpect(status().isOk());
    }

    @Test
    void toggleStatus_success() throws Exception {
        when(productService.toggleStatus(1L)).thenReturn(Map.of("message", "Product status changed to INACTIVE"));
        mockMvc.perform(post("/api/products/1/toggle-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Product status changed to INACTIVE"));
    }

    @Test
    void updateRating_success() throws Exception {
        when(productService.updateRating(1L, 4.5, 100)).thenReturn(productVO);
        mockMvc.perform(post("/api/products/1/rating")
                        .param("rating", "4.5").param("reviewCount", "100"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllCategories_success() throws Exception {
        CategoryVO categoryVO = CategoryVO.builder().categoryId(1L).name("Electronics").build();
        when(productService.getAllCategories()).thenReturn(List.of(categoryVO));
        mockMvc.perform(get("/api/products/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Electronics"));
    }
}
