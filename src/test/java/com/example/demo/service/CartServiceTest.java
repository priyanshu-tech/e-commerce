package com.example.demo.service;

import com.example.demo.config.DiscountConfig;
import com.example.demo.entity.*;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.*;
import com.example.demo.service.impl.CartServiceImpl;
import com.example.demo.vo.cart.CartItemVO;
import com.example.demo.vo.cart.CartVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock CartRepository cartRepository;
    @Mock CartItemRepository cartItemRepository;
    @Mock ProductRepository productRepository;
    @Mock InventoryRepository inventoryRepository;
    @Mock DiscountRepository discountRepository;
    @Mock DiscountConfig discountConfig;
    @InjectMocks CartServiceImpl cartService;

    private Cart cart;
    private Product product;
    private CartItem cartItem;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        cart = Cart.builder().cartId(1L).userId(1L).build();
        product = Product.builder().productId(1L).name("Headphones").sku("HP-001")
                .price(new BigDecimal("999.99")).brand("Sony").status("ACTIVE").build();
        cartItem = CartItem.builder().cartItemId(1L).cartId(1L).productId(1L).quantity(2).build();
        inventory = Inventory.builder().inventoryId(1L).productId(1L).availableQuantity(50).build();
    }

    private void stubBuildCartVO() {
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of(cartItem));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(discountRepository.findActiveDiscount(eq(1L), any())).thenReturn(Optional.empty());
        when(discountConfig.getMaxDiscountPct()).thenReturn(new BigDecimal("90"));
    }

    @Test
    void getCart_existingCart() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        stubBuildCartVO();
        CartVO result = cartService.getCart(1L);
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getTotalItems()).isEqualTo(2);
    }

    @Test
    void getCart_createsNewCart() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(cartRepository.save(any())).thenReturn(cart);
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of());
        CartVO result = cartService.getCart(1L);
        assertThat(result.getItems()).isEmpty();
    }

    @Test
    void addItemToCart_newItem() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(1L, 1L)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any())).thenReturn(cartItem);
        stubBuildCartVO();

        CartItemVO vo = CartItemVO.builder().productId(1L).quantity(2).build();
        CartVO result = cartService.addItemToCart(1L, vo);
        assertThat(result).isNotNull();
    }

    @Test
    void addItemToCart_existingItem_incrementsQuantity() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(1L, 1L)).thenReturn(Optional.of(cartItem));
        when(cartItemRepository.save(any())).thenReturn(cartItem);
        stubBuildCartVO();

        CartItemVO vo = CartItemVO.builder().productId(1L).quantity(3).build();
        CartVO result = cartService.addItemToCart(1L, vo);
        assertThat(result).isNotNull();
        verify(cartItemRepository, atLeastOnce()).save(any());
    }

    @Test
    void addItemToCart_productNotFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> cartService.addItemToCart(1L, CartItemVO.builder().productId(1L).quantity(1).build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addItemToCart_inactiveProduct() {
        product.setStatus("INACTIVE");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        assertThatThrownBy(() -> cartService.addItemToCart(1L, CartItemVO.builder().productId(1L).quantity(1).build()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void addItemToCart_outOfStock() {
        inventory.setAvailableQuantity(0);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
        assertThatThrownBy(() -> cartService.addItemToCart(1L, CartItemVO.builder().productId(1L).quantity(1).build()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void addItemToCart_noInventory() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> cartService.addItemToCart(1L, CartItemVO.builder().productId(1L).quantity(1).build()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void updateCartItem_success() {
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(cartItem));
        when(cartItemRepository.save(any())).thenReturn(cartItem);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        stubBuildCartVO();
        CartVO result = cartService.updateCartItem(1L, 1L, 5);
        assertThat(result).isNotNull();
    }

    @Test
    void updateCartItem_notFound() {
        when(cartItemRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> cartService.updateCartItem(1L, 1L, 5))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removeItemFromCart_success() {
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(cartItem));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of());
        CartVO result = cartService.removeItemFromCart(1L, 1L);
        verify(cartItemRepository).delete(cartItem);
        assertThat(result.getItems()).isEmpty();
    }

    @Test
    void removeItemFromCart_notFound() {
        when(cartItemRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> cartService.removeItemFromCart(1L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void clearCart_success() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        Map<String, String> result = cartService.clearCart(1L);
        verify(cartItemRepository).deleteByCartId(1L);
        assertThat(result.get("status")).isEqualTo("success");
    }

    @Test
    void clearCart_noCart() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        Map<String, String> result = cartService.clearCart(1L);
        verify(cartItemRepository, never()).deleteByCartId(any());
        assertThat(result.get("status")).isEqualTo("success");
    }

    @Test
    void addItemToCart_withDiscount() {
        Discount discount = Discount.builder().discountId(1L).productId(1L)
                .discountPct(new BigDecimal("10")).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(1L, 1L)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any())).thenReturn(cartItem);
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of(cartItem));
        when(discountRepository.findActiveDiscount(eq(1L), any())).thenReturn(Optional.of(discount));
        when(discountConfig.getMaxDiscountPct()).thenReturn(new BigDecimal("90"));

        CartItemVO vo = CartItemVO.builder().productId(1L).quantity(1).build();
        CartVO result = cartService.addItemToCart(1L, vo);
        assertThat(result.getSubtotal()).isLessThan(new BigDecimal("1999.98"));
    }
}
