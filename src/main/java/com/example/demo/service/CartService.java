package com.example.demo.service;

import com.example.demo.vo.cart.CartItemVO;
import com.example.demo.vo.cart.CartVO;

import java.util.Map;

/**
 * Cart Service Interface
 * Handles shopping cart business logic
 */
public interface CartService {

    CartVO getCart(Long userId);

    CartVO addItemToCart(Long userId, CartItemVO cartItemVO);

    CartVO updateCartItem(Long userId, Long cartItemId, Integer quantity);

    CartVO removeItemFromCart(Long userId, Long cartItemId);

    Map<String, String> clearCart(Long userId);
}
