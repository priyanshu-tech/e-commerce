package com.example.demo.vo.product;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    private Long categoryId;
    private String categoryName;
    private List<ProductImageVO> images;
    private Double rating;
    private Integer reviewCount;
    private String status;
    private LocalDateTime createdAt;
}
