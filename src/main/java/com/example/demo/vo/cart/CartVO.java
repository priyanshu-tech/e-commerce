package com.example.demo.vo.cart;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cart Value Object
 * Represents shopping cart information
 */
@Data
@Builder
public class CartVO {
    private Long cartId;
    private Long userId;
    private List<CartItemVO> items;
    private BigDecimal subtotal;
    private Integer totalItems;
}
