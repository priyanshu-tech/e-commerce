package com.example.demo.vo.cart;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Cart Item Value Object
 * Represents individual items in shopping cart
 */
@Data
@Builder
public class CartItemVO {
    private Long cartItemId;
    private Long productId;
    private String productName;
    private String sku;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String imageUrl;
    private Boolean inStock;
}
