package com.example.demo.service.impl;

import com.example.demo.config.DiscountConfig;
import com.example.demo.entity.Cart;
import com.example.demo.entity.CartItem;
import com.example.demo.entity.Discount;
import com.example.demo.entity.Product;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.*;
import com.example.demo.service.CartService;
import com.example.demo.util.LogUtils;
import com.example.demo.vo.cart.CartItemVO;
import com.example.demo.vo.cart.CartVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    @Autowired private final CartRepository cartRepository;
    @Autowired private final CartItemRepository cartItemRepository;
    @Autowired private final ProductRepository productRepository;
    @Autowired private final InventoryRepository inventoryRepository;
    @Autowired private final DiscountRepository discountRepository;
    @Autowired private final DiscountConfig discountConfig;

    @Override
    @Transactional
    public CartVO getCart(Long userId) {
        log.info("Getting cart for userId: {}", userId);
        Cart cart = getOrCreateCart(userId);
        CartVO result = buildCartVO(cart);
        LogUtils.info(log, "Fetched cart", result);
        return result;
    }

    @Override
    @Transactional
    public CartVO addItemToCart(Long userId, CartItemVO cartItemVO) {
        log.info("Adding productId: {} to cart for userId: {}", cartItemVO.getProductId(), userId);
        Product product = productRepository.findById(cartItemVO.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + cartItemVO.getProductId()));

        if (!"ACTIVE".equals(product.getStatus())) {
            throw new IllegalStateException("Product is not available: " + product.getName());
        }

        inventoryRepository.findByProductId(product.getProductId())
                .filter(inv -> inv.getAvailableQuantity() > 0)
                .orElseThrow(() -> new IllegalStateException("Product is out of stock: " + product.getName()));

        Cart cart = getOrCreateCart(userId);

        cartItemRepository.findByCartIdAndProductId(cart.getCartId(), product.getProductId())
                .ifPresentOrElse(existing -> {
                    existing.setQuantity(existing.getQuantity() + cartItemVO.getQuantity());
                    cartItemRepository.save(existing);
                }, () -> {
                    CartItem newItem = CartItem.builder()
                            .cartId(cart.getCartId())
                            .productId(product.getProductId())
                            .quantity(cartItemVO.getQuantity())
                            .build();
                    cartItemRepository.save(newItem);
                });

        CartVO result = buildCartVO(cart);
        LogUtils.info(log, "Cart after add", result);
        return result;
    }

    @Override
    @Transactional
    public CartVO updateCartItem(Long userId, Long cartItemId, Integer quantity) {
        log.info("Updating cartItemId: {} for userId: {}, quantity: {}", cartItemId, userId, quantity);
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));
        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);
        Cart cart = getOrCreateCart(userId);
        CartVO result = buildCartVO(cart);
        LogUtils.info(log, "Cart after update", result);
        return result;
    }

    @Override
    @Transactional
    public CartVO removeItemFromCart(Long userId, Long cartItemId) {
        log.info("Removing cartItemId: {} for userId: {}", cartItemId, userId);
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));
        cartItemRepository.delete(cartItem);
        Cart cart = getOrCreateCart(userId);
        CartVO result = buildCartVO(cart);
        LogUtils.info(log, "Cart after remove", result);
        return result;
    }

    @Override
    @Transactional
    public Map<String, String> clearCart(Long userId) {
        log.info("Clearing cart for userId: {}", userId);
        cartRepository.findByUserId(userId).ifPresent(cart -> {
            cartItemRepository.deleteByCartId(cart.getCartId());
            log.info("Cart cleared for userId: {}", userId);
        });
        return Map.of("status", "success", "message", "Cart cleared successfully");
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            log.info("No cart found for userId: {} — creating new cart", userId);
            return cartRepository.save(Cart.builder().userId(userId).build());
        });
    }

    private CartVO buildCartVO(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartId(cart.getCartId());
        List<CartItemVO> itemVOs = items.stream().map(item -> {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + item.getProductId()));
            Discount discount = discountRepository.findActiveDiscount(product.getProductId(), LocalDateTime.now())
                    .orElse(null);
            return CartMapper.toItemVO(item, product, discount, discountConfig);
        }).collect(Collectors.toList());
        return CartMapper.toCartVO(cart.getCartId(), cart.getUserId(), itemVOs);
    }
}
