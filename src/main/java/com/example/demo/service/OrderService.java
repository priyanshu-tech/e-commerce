package com.example.demo.service;

import com.example.demo.vo.order.OrderVO;

import java.util.List;

/**
 * Order Service Interface
 * Handles order placement and management business logic
 */
public interface OrderService {

    OrderVO createOrder(OrderVO orderVO);

    OrderVO getOrderById(Long orderId);

    List<OrderVO> getUserOrders(Long userId, int page, int size);

    OrderVO updateOrderStatus(Long orderId, String status);

    void cancelOrder(Long orderId);
}
