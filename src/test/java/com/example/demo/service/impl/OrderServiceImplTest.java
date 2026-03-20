package com.example.demo.service.impl;

import com.example.demo.config.DiscountConfig;
import com.example.demo.entity.*;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.*;
import com.example.demo.vo.order.OrderVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock CartRepository cartRepository;
    @Mock CartItemRepository cartItemRepository;
    @Mock ProductRepository productRepository;
    @Mock InventoryRepository inventoryRepository;
    @Mock InventoryReservationRepository reservationRepository;
    @Mock AddressRepository addressRepository;
    @Mock OrderRepository orderRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock DiscountRepository discountRepository;
    @Mock DiscountConfig discountConfig;
    @InjectMocks OrderServiceImpl orderService;

    private Cart cart() {
        return Cart.builder().cartId(1L).userId(1L).build();
    }

    private CartItem cartItem() {
        return CartItem.builder().cartItemId(1L).cartId(1L).productId(1L).quantity(2).build();
    }

    private Product product() {
        return Product.builder().productId(1L).name("Headphones").sku("SKU-001")
                .price(new BigDecimal("999.99")).status("ACTIVE").build();
    }

    private Address address() {
        return Address.builder().addressId(1L).userId(1L)
                .addressLine1("123 Main St").city("Mumbai").state("MH")
                .zipCode("400001").country("India").build();
    }

    private Inventory inventory() {
        return Inventory.builder().inventoryId(1L).productId(1L)
                .availableQuantity(10).reservedQuantity(0).totalQuantity(10).build();
    }

    private Order savedOrder() {
        return Order.builder().orderId(1L).orderNumber("ORD-20260101-0001")
                .userId(1L).totalAmount(new BigDecimal("1999.98"))
                .orderStatus("CONFIRMED").orderDate(LocalDateTime.now())
                .shippingAddressLine1("123 Main St").shippingCity("Mumbai")
                .shippingState("MH").shippingZipCode("400001").shippingCountry("India").build();
    }

    // ── placeOrder ───────────────────────────────────────────

    @Test
    void placeOrder_success() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart()));
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of(cartItem()));
        when(addressRepository.findById(1L)).thenReturn(Optional.of(address()));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product()));
        when(discountRepository.findActiveDiscount(eq(1L), any())).thenReturn(Optional.empty());
        when(orderRepository.save(any())).thenReturn(savedOrder());
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory()));
        when(inventoryRepository.save(any())).thenReturn(inventory());
        when(reservationRepository.save(any())).thenReturn(InventoryReservation.builder().build());
        when(orderItemRepository.save(any())).thenReturn(OrderItem.builder().build());

        OrderVO result = orderService.placeOrder(1L, 1L);

        assertThat(result.getOrderNumber()).startsWith("ORD-");
        assertThat(result.getOrderStatus()).isEqualTo("CONFIRMED");
        verify(cartItemRepository).deleteByCartId(1L);
    }

    @Test
    void placeOrder_cartNotFound_throws() {
        when(cartRepository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.placeOrder(99L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Cart not found");
    }

    @Test
    void placeOrder_emptyCart_throws() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart()));
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> orderService.placeOrder(1L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cart is empty");
    }

    @Test
    void placeOrder_addressNotFound_throws() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart()));
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of(cartItem()));
        when(addressRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.placeOrder(1L, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Address not found");
    }

    @Test
    void placeOrder_inactiveProduct_throws() {
        Product inactive = product();
        inactive.setStatus("INACTIVE");

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart()));
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of(cartItem()));
        when(addressRepository.findById(1L)).thenReturn(Optional.of(address()));
        when(productRepository.findById(1L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> orderService.placeOrder(1L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no longer available");
    }

    // ── cancelOrder ──────────────────────────────────────────

    @Test
    void cancelOrder_success() {
        Order order = savedOrder();
        InventoryReservation reservation = InventoryReservation.builder()
                .reservationId(1L).inventoryId(1L).orderId(1L).quantity(2).build();

        when(orderRepository.findByOrderNumber("ORD-20260101-0001")).thenReturn(Optional.of(order));
        when(reservationRepository.findByOrderId(1L)).thenReturn(List.of(reservation));
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory()));
        when(inventoryRepository.save(any())).thenReturn(inventory());
        when(orderRepository.save(any())).thenReturn(order);

        var result = orderService.cancelOrder("ORD-20260101-0001");

        assertThat(result.get("status")).isEqualTo("success");
        verify(orderRepository).save(argThat(o -> "CANCELLED".equals(o.getOrderStatus())));
        verify(reservationRepository).delete(reservation);
    }

    @Test
    void cancelOrder_notFound_throws() {
        when(orderRepository.findByOrderNumber("INVALID")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelOrder("INVALID"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cancelOrder_notConfirmed_throws() {
        Order order = savedOrder();
        order.setOrderStatus("CANCELLED");
        when(orderRepository.findByOrderNumber("ORD-20260101-0001")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder("ORD-20260101-0001"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only CONFIRMED");
    }

    // ── getOrderByNumber ─────────────────────────────────────

    @Test
    void getOrderByNumber_success() {
        Order order = savedOrder();
        when(orderRepository.findByOrderNumber("ORD-20260101-0001")).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of());

        OrderVO result = orderService.getOrderByNumber("ORD-20260101-0001");

        assertThat(result.getOrderNumber()).isEqualTo("ORD-20260101-0001");
    }

    @Test
    void getOrderByNumber_notFound_throws() {
        when(orderRepository.findByOrderNumber("INVALID")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderByNumber("INVALID"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
