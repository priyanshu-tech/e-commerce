package com.example.demo.vo.product;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Product Value Object
 * Represents product catalog information
 */
@Data
@Builder
public class ProductVO {
    private Long productId;
    private String name;
    private String description;
    private String sku;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private String brand;
    private String category;
    private List<String> imageUrls;
    private Double rating;
    private Integer reviewCount;
    private String status;
    private LocalDateTime createdAt;
}
