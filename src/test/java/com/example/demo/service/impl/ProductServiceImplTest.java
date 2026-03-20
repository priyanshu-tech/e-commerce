package com.example.demo.service.impl;

import com.example.demo.config.DiscountConfig;
import com.example.demo.entity.Product;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.*;
import com.example.demo.vo.product.ProductVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock ProductRepository productRepository;
    @Mock ProductImageRepository productImageRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock DiscountRepository discountRepository;
    @Mock DiscountConfig discountConfig;
    @InjectMocks ProductServiceImpl productService;

    private Product buildProduct(Long id, String status) {
        return Product.builder()
                .productId(id).name("Headphones").sku("SKU-001")
                .price(new BigDecimal("999.99")).brand("Sony")
                .categoryId(1L).status(status).reviewCount(0).build();
    }

    // ── createProduct ────────────────────────────────────────

    @Test
    void createProduct_success() {
        ProductVO vo = ProductVO.builder().name("Headphones").sku("SKU-001")
                .price(new BigDecimal("999.99")).brand("Sony").categoryId(1L).build();
        Product saved = buildProduct(1L, "ACTIVE");

        when(productRepository.existsBySku("SKU-001")).thenReturn(false);
        when(productRepository.save(any())).thenReturn(saved);
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(1L)).thenReturn(List.of());
        when(discountRepository.findActiveDiscount(eq(1L), any())).thenReturn(Optional.empty());
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        ProductVO result = productService.createProduct(vo);

        assertThat(result.getProductId()).isEqualTo(1L);
        assertThat(result.getSku()).isEqualTo("SKU-001");
    }

    @Test
    void createProduct_duplicateSku_throws() {
        ProductVO vo = ProductVO.builder().sku("SKU-001").build();
        when(productRepository.existsBySku("SKU-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(vo))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("SKU-001");
    }

    // ── getProductById ───────────────────────────────────────

    @Test
    void getProductById_success() {
        Product product = buildProduct(1L, "ACTIVE");
        when(productRepository.findByProductIdAndStatus(1L, "ACTIVE")).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(1L)).thenReturn(List.of());
        when(discountRepository.findActiveDiscount(eq(1L), any())).thenReturn(Optional.empty());
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        ProductVO result = productService.getProductById(1L);

        assertThat(result.getProductId()).isEqualTo(1L);
    }

    @Test
    void getProductById_notFound_throws() {
        when(productRepository.findByProductIdAndStatus(99L, "ACTIVE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── toggleStatus ─────────────────────────────────────────

    @Test
    void toggleStatus_activeToInactive() {
        Product product = buildProduct(1L, "ACTIVE");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenReturn(product);

        var result = productService.toggleStatus(1L);

        assertThat(result.get("message")).contains("INACTIVE");
        verify(productRepository).save(argThat(p -> "INACTIVE".equals(p.getStatus())));
    }

    @Test
    void toggleStatus_inactiveToActive() {
        Product product = buildProduct(1L, "INACTIVE");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenReturn(product);

        var result = productService.toggleStatus(1L);

        assertThat(result.get("message")).contains("ACTIVE");
        verify(productRepository).save(argThat(p -> "ACTIVE".equals(p.getStatus())));
    }

    @Test
    void toggleStatus_notFound_throws() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.toggleStatus(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
