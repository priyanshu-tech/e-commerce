package com.example.demo.service;

import com.example.demo.vo.order.OrderVO;

import java.util.List;
import java.util.Map;

public interface OrderService {

    OrderVO placeOrder(Long userId, Long addressId);

    OrderVO getOrderByNumber(String orderNumber);

    List<OrderVO> getUserOrders(Long userId, int page, int size);

    Map<String, String> cancelOrder(String orderNumber);
}
