package com.example.demo.service.impl;

import com.example.demo.service.CartService;
import com.example.demo.util.LogUtils;
import com.example.demo.vo.cart.CartItemVO;
import com.example.demo.vo.cart.CartVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    @Override
    public CartVO getCart(Long userId) {
        log.info("Getting cart for userId: {}", userId);
        CartVO result = CartVO.builder().userId(userId).build();
        LogUtils.info(log, "Fetched cart", result);
        return result;
    }

    @Override
    public CartVO addItemToCart(Long userId, CartItemVO cartItemVO) {
        log.info("Adding item to cart for userId: {}", userId);
        LogUtils.info(log, "Cart item payload", cartItemVO);
        CartVO result = CartVO.builder().userId(userId).build();
        LogUtils.info(log, "Cart after add", result);
        return result;
    }

    @Override
    public CartVO updateCartItem(Long userId, Long cartItemId, Integer quantity) {
        log.info("Updating cartItemId: {} for userId: {}, quantity: {}", cartItemId, userId, quantity);
        CartVO result = CartVO.builder().userId(userId).build();
        LogUtils.info(log, "Cart after update", result);
        return result;
    }

    @Override
    public CartVO removeItemFromCart(Long userId, Long cartItemId) {
        log.info("Removing cartItemId: {} for userId: {}", cartItemId, userId);
        CartVO result = CartVO.builder().userId(userId).build();
        LogUtils.info(log, "Cart after remove", result);
        return result;
    }

    @Override
    public void clearCart(Long userId) {
        log.info("Clearing cart for userId: {}", userId);
    }
}
