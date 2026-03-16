package com.example.demo.vo.product;

import lombok.Builder;
import lombok.Data;

/**
 * Category Value Object
 * Represents product category hierarchy
 */
@Data
@Builder
public class CategoryVO {
    private Long categoryId;
    private String name;
    private String description;
    private Long parentCategoryId;
    private String imageUrl;
    private Integer displayOrder;
}
