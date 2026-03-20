package com.example.demo.service.impl;

import com.example.demo.config.DiscountConfig;
import com.example.demo.entity.*;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.*;
import com.example.demo.vo.cart.CartItemVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock CartRepository cartRepository;
    @Mock CartItemRepository cartItemRepository;
    @Mock ProductRepository productRepository;
    @Mock InventoryRepository inventoryRepository;
    @Mock DiscountRepository discountRepository;
    @Mock DiscountConfig discountConfig;
    @InjectMocks CartServiceImpl cartService;

    private Product activeProduct() {
        return Product.builder().productId(1L).name("Headphones").sku("SKU-001")
                .price(new BigDecimal("999.99")).status("ACTIVE").reviewCount(0).build();
    }

    private Cart cart() {
        return Cart.builder().cartId(1L).userId(1L).build();
    }

    private Inventory inventory(int available) {
        return Inventory.builder().inventoryId(1L).productId(1L)
                .availableQuantity(available).reservedQuantity(0).totalQuantity(available).build();
    }

    // ── addItemToCart ────────────────────────────────────────

    @Test
    void addItemToCart_newItem_success() {
        CartItemVO vo = CartItemVO.builder().productId(1L).quantity(2).build();
        CartItem saved = CartItem.builder().cartItemId(1L).cartId(1L).productId(1L).quantity(2).build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(activeProduct()));
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory(10)));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart()));
        when(cartItemRepository.findByCartIdAndProductId(1L, 1L)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any())).thenReturn(saved);
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of(saved));
        when(discountRepository.findActiveDiscount(eq(1L), any())).thenReturn(Optional.empty());

        var result = cartService.addItemToCart(1L, vo);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getTotalItems()).isEqualTo(2);
    }

    @Test
    void addItemToCart_duplicateProduct_incrementsQuantity() {
        CartItemVO vo = CartItemVO.builder().productId(1L).quantity(2).build();
        CartItem existing = CartItem.builder().cartItemId(1L).cartId(1L).productId(1L).quantity(3).build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(activeProduct()));
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory(10)));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart()));
        when(cartItemRepository.findByCartIdAndProductId(1L, 1L)).thenReturn(Optional.of(existing));
        when(cartItemRepository.save(any())).thenReturn(existing);
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of(existing));
        when(discountRepository.findActiveDiscount(eq(1L), any())).thenReturn(Optional.empty());

        cartService.addItemToCart(1L, vo);

        verify(cartItemRepository).save(argThat(item -> item.getQuantity() == 5));
    }

    @Test
    void addItemToCart_productNotFound_throws() {
        CartItemVO vo = CartItemVO.builder().productId(99L).quantity(1).build();
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItemToCart(1L, vo))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addItemToCart_inactiveProduct_throws() {
        CartItemVO vo = CartItemVO.builder().productId(1L).quantity(1).build();
        Product inactive = activeProduct();
        inactive.setStatus("INACTIVE");
        when(productRepository.findById(1L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> cartService.addItemToCart(1L, vo))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void addItemToCart_outOfStock_throws() {
        CartItemVO vo = CartItemVO.builder().productId(1L).quantity(1).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(activeProduct()));
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory(0)));

        assertThatThrownBy(() -> cartService.addItemToCart(1L, vo))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("out of stock");
    }

    // ── removeItemFromCart ───────────────────────────────────

    @Test
    void removeItemFromCart_success() {
        CartItem item = CartItem.builder().cartItemId(1L).cartId(1L).productId(1L).quantity(2).build();
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart()));
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of());

        var result = cartService.removeItemFromCart(1L, 1L);

        verify(cartItemRepository).delete(item);
        assertThat(result.getItems()).isEmpty();
    }

    @Test
    void removeItemFromCart_notFound_throws() {
        when(cartItemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeItemFromCart(1L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── clearCart ────────────────────────────────────────────

    @Test
    void clearCart_success() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart()));

        var result = cartService.clearCart(1L);

        verify(cartItemRepository).deleteByCartId(1L);
        assertThat(result.get("status")).isEqualTo("success");
    }

    @Test
    void clearCart_noCart_returnsSuccess() {
        when(cartRepository.findByUserId(99L)).thenReturn(Optional.empty());

        var result = cartService.clearCart(99L);

        verify(cartItemRepository, never()).deleteByCartId(any());
        assertThat(result.get("status")).isEqualTo("success");
    }
}
