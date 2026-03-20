package com.example.demo.service;

import com.example.demo.config.DiscountConfig;
import com.example.demo.entity.*;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.*;
import com.example.demo.service.impl.OrderServiceImpl;
import com.example.demo.vo.order.OrderVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

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

    private Cart cart;
    private CartItem cartItem;
    private Product product;
    private Address address;
    private Order order;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        cart = Cart.builder().cartId(1L).userId(1L).build();
        cartItem = CartItem.builder().cartItemId(1L).cartId(1L).productId(1L).quantity(2).build();
        product = Product.builder().productId(1L).name("Headphones").sku("HP-001")
                .price(new BigDecimal("999.99")).brand("Sony").status("ACTIVE").reviewCount(0).build();
        address = Address.builder().addressId(1L).userId(1L)
                .addressLine1("123 St").city("Mumbai").state("MH")
                .zipCode("400001").country("India").addressType("SHIPPING").isDefault(true).build();
        inventory = Inventory.builder().inventoryId(1L).productId(1L)
                .availableQuantity(50).reservedQuantity(0).build();
        order = Order.builder().orderId(1L).orderNumber("ORD-20250101-0001")
                .userId(1L).totalAmount(new BigDecimal("1999.98"))
                .orderStatus("CONFIRMED").orderDate(LocalDateTime.now())
                .shippingAddressLine1("123 St").shippingCity("Mumbai")
                .shippingState("MH").shippingZipCode("400001").shippingCountry("India").build();
    }

    @Test
    void placeOrder_success() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of(cartItem));
        when(addressRepository.findById(1L)).thenReturn(Optional.of(address));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(discountRepository.findActiveDiscount(eq(1L), any())).thenReturn(Optional.empty());
        when(discountConfig.getMaxDiscountPct()).thenReturn(new BigDecimal("90"));
        when(orderRepository.save(any())).thenReturn(order);
        when(orderItemRepository.save(any())).thenReturn(OrderItem.builder().orderItemId(1L)
                .orderId(1L).productId(1L).productName("Headphones").sku("HP-001")
                .quantity(2).originalPrice(new BigDecimal("999.99"))
                .discountedPrice(new BigDecimal("999.99")).finalPrice(new BigDecimal("1999.98")).build());
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
        when(reservationRepository.save(any())).thenReturn(InventoryReservation.builder()
                .reservationId(1L).inventoryId(1L).orderId(1L).quantity(2)
                .expiresAt(LocalDateTime.now().plusMinutes(15)).build());
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of());

        OrderVO result = orderService.placeOrder(1L, 1L);
        assertThat(result.getOrderNumber()).startsWith("ORD-");
        verify(cartItemRepository).deleteByCartId(1L);
    }

    @Test
    void placeOrder_cartNotFound() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderService.placeOrder(1L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void placeOrder_emptyCart() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of());
        assertThatThrownBy(() -> orderService.placeOrder(1L, 1L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void placeOrder_addressNotFound() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of(cartItem));
        when(addressRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderService.placeOrder(1L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void placeOrder_inactiveProduct() {
        product.setStatus("INACTIVE");
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of(cartItem));
        when(addressRepository.findById(1L)).thenReturn(Optional.of(address));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(discountRepository.findActiveDiscount(eq(1L), any())).thenReturn(Optional.empty());
        when(discountConfig.getMaxDiscountPct()).thenReturn(new BigDecimal("90"));
        assertThatThrownBy(() -> orderService.placeOrder(1L, 1L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void placeOrder_insufficientStock() {
        inventory.setAvailableQuantity(1);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of(cartItem));
        when(addressRepository.findById(1L)).thenReturn(Optional.of(address));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(discountRepository.findActiveDiscount(eq(1L), any())).thenReturn(Optional.empty());
        when(discountConfig.getMaxDiscountPct()).thenReturn(new BigDecimal("90"));
        when(orderRepository.save(any())).thenReturn(order);
        when(orderItemRepository.save(any())).thenReturn(OrderItem.builder().orderItemId(1L)
                .orderId(1L).productId(1L).productName("Headphones").sku("HP-001")
                .quantity(2).originalPrice(new BigDecimal("999.99"))
                .discountedPrice(new BigDecimal("999.99")).finalPrice(new BigDecimal("1999.98")).build());
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
        assertThatThrownBy(() -> orderService.placeOrder(1L, 1L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void placeOrder_withDiscount() {
        Discount discount = Discount.builder().discountId(1L).productId(1L)
                .discountPct(new BigDecimal("10")).build();
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of(cartItem));
        when(addressRepository.findById(1L)).thenReturn(Optional.of(address));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(discountRepository.findActiveDiscount(eq(1L), any())).thenReturn(Optional.of(discount));
        when(discountConfig.getMaxDiscountPct()).thenReturn(new BigDecimal("90"));
        when(orderRepository.save(any())).thenReturn(order);
        when(orderItemRepository.save(any())).thenReturn(OrderItem.builder().orderItemId(1L)
                .orderId(1L).productId(1L).productName("Headphones").sku("HP-001")
                .quantity(2).originalPrice(new BigDecimal("999.99"))
                .discountedPrice(new BigDecimal("899.99")).finalPrice(new BigDecimal("1799.98")).build());
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
        when(reservationRepository.save(any())).thenReturn(InventoryReservation.builder()
                .reservationId(1L).inventoryId(1L).orderId(1L).quantity(2)
                .expiresAt(LocalDateTime.now().plusMinutes(15)).build());
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of());

        OrderVO result = orderService.placeOrder(1L, 1L);
        assertThat(result).isNotNull();
    }

    @Test
    void getOrderByNumber_success() {
        when(orderRepository.findByOrderNumber("ORD-20250101-0001")).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of());
        OrderVO result = orderService.getOrderByNumber("ORD-20250101-0001");
        assertThat(result.getOrderNumber()).isEqualTo("ORD-20250101-0001");
    }

    @Test
    void getOrderByNumber_notFound() {
        when(orderRepository.findByOrderNumber("UNKNOWN")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderService.getOrderByNumber("UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getUserOrders_success() {
        when(orderRepository.findByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order)));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of());
        List<OrderVO> result = orderService.getUserOrders(1L, 0, 10);
        assertThat(result).hasSize(1);
    }

    @Test
    void cancelOrder_success() {
        InventoryReservation reservation = InventoryReservation.builder()
                .reservationId(1L).inventoryId(1L).orderId(1L).quantity(2).build();
        when(orderRepository.findByOrderNumber("ORD-20250101-0001")).thenReturn(Optional.of(order));
        when(reservationRepository.findByOrderId(1L)).thenReturn(List.of(reservation));
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any())).thenReturn(inventory);
        when(orderRepository.save(any())).thenReturn(order);

        Map<String, String> result = orderService.cancelOrder("ORD-20250101-0001");
        assertThat(result.get("status")).isEqualTo("success");
        verify(reservationRepository).delete(reservation);
    }

    @Test
    void cancelOrder_notFound() {
        when(orderRepository.findByOrderNumber("UNKNOWN")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderService.cancelOrder("UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cancelOrder_notConfirmed() {
        order.setOrderStatus("CANCELLED");
        when(orderRepository.findByOrderNumber("ORD-20250101-0001")).thenReturn(Optional.of(order));
        assertThatThrownBy(() -> orderService.cancelOrder("ORD-20250101-0001"))
                .isInstanceOf(IllegalStateException.class);
    }
}
