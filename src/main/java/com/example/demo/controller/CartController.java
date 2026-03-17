package com.example.demo.controller;

import com.example.demo.service.CartService;
import com.example.demo.vo.cart.CartItemVO;
import com.example.demo.vo.cart.CartVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Cart Controller
 * Handles shopping cart operations
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    @Autowired
    private final CartService cartService;

    @GetMapping("/{userId}")
    public CartVO getCart(@PathVariable Long userId) {
        return cartService.getCart(userId);
    }

    @PostMapping("/{userId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartVO addItemToCart(@PathVariable Long userId, @RequestBody CartItemVO cartItemVO) {
        return cartService.addItemToCart(userId, cartItemVO);
    }

    @PostMapping("/{userId}/items/{cartItemId}")
    public CartVO updateCartItem(
            @PathVariable Long userId,
            @PathVariable Long cartItemId,
            @RequestParam Integer quantity) {
        return cartService.updateCartItem(userId, cartItemId, quantity);
    }

    @PostMapping("/{userId}/items/{cartItemId}/remove")
    public CartVO removeItemFromCart(@PathVariable Long userId, @PathVariable Long cartItemId) {
        return cartService.removeItemFromCart(userId, cartItemId);
    }

    @PostMapping("/{userId}/clear")
    public Map<String, String> clearCart(@PathVariable Long userId) {
        return cartService.clearCart(userId);
    }
}
