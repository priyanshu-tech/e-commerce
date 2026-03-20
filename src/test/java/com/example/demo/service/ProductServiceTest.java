package com.example.demo.service;

import com.example.demo.config.DiscountConfig;
import com.example.demo.entity.*;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.*;
import com.example.demo.service.impl.ProductServiceImpl;
import com.example.demo.vo.product.ProductVO;
import com.example.demo.vo.product.CategoryVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock ProductRepository productRepository;
    @Mock ProductImageRepository productImageRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock DiscountRepository discountRepository;
    @Mock DiscountConfig discountConfig;
    @InjectMocks ProductServiceImpl productService;

    private Product product;
    private Category category;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .productId(1L).name("Headphones").sku("HP-001")
                .price(new BigDecimal("999.99")).brand("Sony")
                .categoryId(1L).status("ACTIVE").reviewCount(0)
                .createdAt(LocalDateTime.now()).build();

        category = Category.builder().categoryId(1L).name("Electronics")
                .displayOrder(1).build();
    }

    @Test
    void getAllProducts_noFilter() {
        when(productRepository.findAllActive(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product)));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(1L)).thenReturn(List.of());
        when(discountRepository.findActiveDiscount(eq(1L), any())).thenReturn(Optional.empty());
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(discountConfig.getMaxDiscountPct()).thenReturn(new BigDecimal("90"));

        List<ProductVO> result = productService.getAllProducts(null, null, 0, 20);
        assertThat(result).hasSize(1);
    }

    @Test
    void getAllProducts_withCategory() {
        when(categoryRepository.findByName("Electronics")).thenReturn(Optional.of(category));
        when(productRepository.findAllActive(eq(1L), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product)));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(1L)).thenReturn(List.of());
        when(discountRepository.findActiveDiscount(eq(1L), any())).thenReturn(Optional.empty());
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(discountConfig.getMaxDiscountPct()).thenReturn(new BigDecimal("90"));

        List<ProductVO> result = productService.getAllProducts("Electronics", null, 0, 20);
        assertThat(result).hasSize(1);
    }

    @Test
    void getAllProducts_categoryNotFound() {
        when(categoryRepository.findByName("Unknown")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> productService.getAllProducts("Unknown", null, 0, 20))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getProductById_success() {
        when(productRepository.findByProductIdAndStatus(1L, "ACTIVE")).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(1L)).thenReturn(List.of());
        when(discountRepository.findActiveDiscount(eq(1L), any())).thenReturn(Optional.empty());
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(discountConfig.getMaxDiscountPct()).thenReturn(new BigDecimal("90"));

        ProductVO result = productService.getProductById(1L);
        assertThat(result.getSku()).isEqualTo("HP-001");
    }

    @Test
    void getProductById_notFound() {
        when(productRepository.findByProductIdAndStatus(1L, "ACTIVE")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> productService.getProductById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createProduct_success() {
        when(productRepository.existsBySku("HP-001")).thenReturn(false);
        when(productRepository.save(any())).thenReturn(product);
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(1L)).thenReturn(List.of());
        when(discountRepository.findActiveDiscount(eq(1L), any())).thenReturn(Optional.empty());
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(discountConfig.getMaxDiscountPct()).thenReturn(new BigDecimal("90"));

        ProductVO vo = ProductVO.builder().name("Headphones").sku("HP-001")
                .price(new BigDecimal("999.99")).brand("Sony").categoryId(1L).status("ACTIVE").build();
        ProductVO result = productService.createProduct(vo);
        assertThat(result.getSku()).isEqualTo("HP-001");
    }

    @Test
    void createProduct_duplicateSku() {
        when(productRepository.existsBySku("HP-001")).thenReturn(true);
        ProductVO vo = ProductVO.builder().sku("HP-001").build();
        assertThatThrownBy(() -> productService.createProduct(vo))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void updateProduct_success() {
        when(productRepository.findByProductIdAndStatus(1L, "ACTIVE")).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenReturn(product);
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(1L)).thenReturn(List.of());
        when(discountRepository.findActiveDiscount(eq(1L), any())).thenReturn(Optional.empty());
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(discountConfig.getMaxDiscountPct()).thenReturn(new BigDecimal("90"));

        ProductVO vo = ProductVO.builder().name("Updated").sku("HP-001")
                .price(new BigDecimal("899.99")).brand("Sony").categoryId(1L).build();
        ProductVO result = productService.updateProduct(1L, vo);
        assertThat(result).isNotNull();
    }

    @Test
    void updateProduct_notFound() {
        when(productRepository.findByProductIdAndStatus(1L, "ACTIVE")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> productService.updateProduct(1L, ProductVO.builder().build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void toggleStatus_activeToInactive() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenReturn(product);
        Map<String, String> result = productService.toggleStatus(1L);
        assertThat(result.get("message")).contains("INACTIVE");
    }

    @Test
    void toggleStatus_inactiveToActive() {
        product.setStatus("INACTIVE");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenReturn(product);
        Map<String, String> result = productService.toggleStatus(1L);
        assertThat(result.get("message")).contains("ACTIVE");
    }

    @Test
    void toggleStatus_notFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> productService.toggleStatus(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateRating_success() {
        when(productRepository.findByProductIdAndStatus(1L, "ACTIVE")).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenReturn(product);
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(1L)).thenReturn(List.of());
        when(discountRepository.findActiveDiscount(eq(1L), any())).thenReturn(Optional.empty());
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(discountConfig.getMaxDiscountPct()).thenReturn(new BigDecimal("90"));

        ProductVO result = productService.updateRating(1L, 4.5, 100);
        assertThat(result).isNotNull();
    }

    @Test
    void getAllCategories_success() {
        when(categoryRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of(category));
        List<CategoryVO> result = productService.getAllCategories();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Electronics");
    }

    @Test
    void enrichProduct_withDiscount_exceedsMax() {
        Discount discount = Discount.builder()
                .discountId(1L).productId(1L)
                .discountPct(new BigDecimal("95")).build();
        when(productRepository.findByProductIdAndStatus(1L, "ACTIVE")).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(1L)).thenReturn(List.of());
        when(discountRepository.findActiveDiscount(eq(1L), any())).thenReturn(Optional.of(discount));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(discountConfig.getMaxDiscountPct()).thenReturn(new BigDecimal("90"));

        ProductVO result = productService.getProductById(1L);
        assertThat(result.getDiscountPrice()).isNotNull();
    }
}
