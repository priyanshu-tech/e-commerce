package com.example.demo.vo.order;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Order Item Value Object
 * Represents individual items in an order
 */
@Data
@Builder
public class OrderItemVO {
    private Long orderItemId;
    private Long productId;
    private String productName;
    private String sku;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String imageUrl;
}
