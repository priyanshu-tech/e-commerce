package com.example.demo.service.impl;

import com.example.demo.config.DiscountConfig;
import com.example.demo.entity.*;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.*;
import com.example.demo.service.OrderService;
import com.example.demo.util.LogUtils;
import com.example.demo.vo.order.OrderItemVO;
import com.example.demo.vo.order.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DiscountRepository discountRepository;
    private final DiscountConfig discountConfig;

    @Override
    @Transactional
    public OrderVO placeOrder(Long userId, Long addressId) {
        log.info("Placing order for userId: {}, addressId: {}", userId, addressId);

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for userId: " + userId));

        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getCartId());
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Cannot place order — cart is empty");
        }

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));

        // Build order items + compute total
        List<OrderItem> orderItems = cartItems.stream().map(cartItem -> {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + cartItem.getProductId()));

            if (!"ACTIVE".equals(product.getStatus())) {
                throw new IllegalStateException("Product is no longer available: " + product.getName());
            }

            Discount discount = discountRepository.findActiveDiscount(product.getProductId(), LocalDateTime.now())
                    .orElse(null);

            BigDecimal originalPrice = product.getPrice();
            BigDecimal discountedPrice = originalPrice;
            if (discount != null) {
                BigDecimal pct = discount.getDiscountPct().min(discountConfig.getMaxDiscountPct());
                BigDecimal multiplier = BigDecimal.ONE.subtract(pct.divide(BigDecimal.valueOf(100)));
                discountedPrice = originalPrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
            }
            BigDecimal finalPrice = discountedPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            return OrderItem.builder()
                    .productId(product.getProductId())
                    .productName(product.getName())
                    .sku(product.getSku())
                    .quantity(cartItem.getQuantity())
                    .originalPrice(originalPrice)
                    .discountedPrice(discountedPrice)
                    .finalPrice(finalPrice)
                    .build();
        }).collect(Collectors.toList());

        BigDecimal totalAmount = orderItems.stream()
                .map(OrderItem::getFinalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Generate orderNumber using sequence value placeholder — set after save
        Order order = Order.builder()
                .userId(userId)
                .totalAmount(totalAmount)
                .orderStatus("CONFIRMED")
                .shippingAddressLine1(address.getAddressLine1())
                .shippingAddressLine2(address.getAddressLine2())
                .shippingCity(address.getCity())
                .shippingState(address.getState())
                .shippingZipCode(address.getZipCode())
                .shippingCountry(address.getCountry())
                .build();

        // Save order first to get orderId (from sequence)
        order.setOrderNumber("TEMP");
        Order savedOrder = orderRepository.save(order);

        // Generate orderNumber: ORD-YYYYMMDD-{orderId padded to 4 digits}
        String orderNumber = "ORD-" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) +
                "-" + String.format("%04d", savedOrder.getOrderId());
        savedOrder.setOrderNumber(orderNumber);
        orderRepository.save(savedOrder);

        // Save order items + reserve inventory
        for (OrderItem item : orderItems) {
            item.setOrderId(savedOrder.getOrderId());
            orderItemRepository.save(item);

            Inventory inventory = inventoryRepository.findByProductId(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for productId: " + item.getProductId()));

            if (inventory.getAvailableQuantity() < item.getQuantity()) {
                throw new IllegalStateException("Insufficient stock for product: " + item.getProductName());
            }

            inventory.setAvailableQuantity(inventory.getAvailableQuantity() - item.getQuantity());
            inventory.setReservedQuantity(inventory.getReservedQuantity() + item.getQuantity());
            inventoryRepository.save(inventory);

            InventoryReservation reservation = InventoryReservation.builder()
                    .inventoryId(inventory.getInventoryId())
                    .orderId(savedOrder.getOrderId())
                    .quantity(item.getQuantity())
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .build();
            reservationRepository.save(reservation);
        }

        // Clear cart
        cartItemRepository.deleteByCartId(cart.getCartId());

        OrderVO result = toOrderVO(savedOrder, orderItems);
        LogUtils.info(log, "Order placed", result);
        return result;
    }

    @Override
    public OrderVO getOrderByNumber(String orderNumber) {
        log.info("Fetching order: {}", orderNumber);
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderNumber));
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getOrderId());
        OrderVO result = toOrderVO(order, items);
        LogUtils.info(log, "Fetched order", result);
        return result;
    }

    @Override
    public List<OrderVO> getUserOrders(Long userId, int page, int size) {
        log.info("Fetching orders for userId: {}", userId);
        return orderRepository.findByUserId(userId, PageRequest.of(page, size, Sort.by("orderDate").descending()))
                .stream()
                .map(order -> toOrderVO(order, orderItemRepository.findByOrderId(order.getOrderId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Map<String, String> cancelOrder(String orderNumber) {
        log.info("Cancelling order: {}", orderNumber);
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderNumber));

        if (!"CONFIRMED".equals(order.getOrderStatus())) {
            throw new IllegalStateException("Only CONFIRMED orders can be cancelled. Current status: " + order.getOrderStatus());
        }

        List<InventoryReservation> reservations = reservationRepository.findByOrderId(order.getOrderId());
        for (InventoryReservation reservation : reservations) {
            Inventory inventory = inventoryRepository.findById(reservation.getInventoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory not found with id: " + reservation.getInventoryId()));
            inventory.setAvailableQuantity(inventory.getAvailableQuantity() + reservation.getQuantity());
            inventory.setReservedQuantity(inventory.getReservedQuantity() - reservation.getQuantity());
            inventoryRepository.save(inventory);
            reservationRepository.delete(reservation);
        }

        order.setOrderStatus("CANCELLED");
        orderRepository.save(order);

        log.info("Order {} cancelled successfully", orderNumber);
        return Map.of("status", "success", "message", "Order " + orderNumber + " cancelled successfully");
    }

    private OrderVO toOrderVO(Order order, List<OrderItem> items) {
        List<OrderItemVO> itemVOs = items.stream().map(item -> OrderItemVO.builder()
                .orderItemId(item.getOrderItemId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .sku(item.getSku())
                .quantity(item.getQuantity())
                .originalPrice(item.getOriginalPrice())
                .discountedPrice(item.getDiscountedPrice())
                .finalPrice(item.getFinalPrice())
                .build()).collect(Collectors.toList());

        return OrderVO.builder()
                .orderId(order.getOrderId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .items(itemVOs)
                .totalAmount(order.getTotalAmount())
                .orderStatus(order.getOrderStatus())
                .orderDate(order.getOrderDate())
                .shippingAddressLine1(order.getShippingAddressLine1())
                .shippingAddressLine2(order.getShippingAddressLine2())
                .shippingCity(order.getShippingCity())
                .shippingState(order.getShippingState())
                .shippingZipCode(order.getShippingZipCode())
                .shippingCountry(order.getShippingCountry())
                .build();
    }
}
