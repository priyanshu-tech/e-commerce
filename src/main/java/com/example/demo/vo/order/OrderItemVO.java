package com.example.demo.vo.order;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderItemVO {
    private Long orderItemId;
    private Long productId;
    private String productName;
    private String sku;
    private Integer quantity;
    private BigDecimal originalPrice;
    private BigDecimal discountedPrice;
    private BigDecimal finalPrice;
}
