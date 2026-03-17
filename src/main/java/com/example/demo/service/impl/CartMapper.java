package com.example.demo.service.impl;

import com.example.demo.config.DiscountConfig;
import com.example.demo.entity.CartItem;
import com.example.demo.entity.Discount;
import com.example.demo.entity.Product;
import com.example.demo.vo.cart.CartItemVO;
import com.example.demo.vo.cart.CartVO;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
class CartMapper {

    static CartItemVO toItemVO(CartItem cartItem, Product product, Discount discount, DiscountConfig discountConfig) {
        BigDecimal originalPrice = product.getPrice();
        BigDecimal unitPrice = originalPrice;

        if (discount != null) {
            BigDecimal pct = discount.getDiscountPct().min(discountConfig.getMaxDiscountPct());
            BigDecimal multiplier = BigDecimal.ONE.subtract(pct.divide(BigDecimal.valueOf(100)));
            unitPrice = originalPrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

        return CartItemVO.builder()
                .cartItemId(cartItem.getCartItemId())
                .productId(product.getProductId())
                .productName(product.getName())
                .sku(product.getSku())
                .quantity(cartItem.getQuantity())
                .unitPrice(unitPrice)
                .totalPrice(totalPrice)
                .build();
    }

    static CartVO toCartVO(Long cartId, Long userId, List<CartItemVO> items) {
        BigDecimal subtotal = items.stream()
                .map(CartItemVO::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalItems = items.stream()
                .mapToInt(CartItemVO::getQuantity)
                .sum();
        return CartVO.builder()
                .cartId(cartId)
                .userId(userId)
                .items(items)
                .subtotal(subtotal)
                .totalItems(totalItems)
                .build();
    }
}
