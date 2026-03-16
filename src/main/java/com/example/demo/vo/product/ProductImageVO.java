package com.example.demo.vo.product;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductImageVO {
    private Long imageId;
    private Long productId;
    private String imageUrl;
    private Integer displayOrder;
    private Boolean isPrimary;
}
