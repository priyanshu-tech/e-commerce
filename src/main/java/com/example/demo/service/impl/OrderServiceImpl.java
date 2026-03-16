package com.example.demo.service.impl;

import com.example.demo.service.OrderService;
import com.example.demo.util.LogUtils;
import com.example.demo.vo.order.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Override
    public OrderVO createOrder(OrderVO orderVO) {
        LogUtils.info(log, "Creating order", orderVO);
        LogUtils.info(log, "Order created", orderVO);
        return orderVO;
    }

    @Override
    public OrderVO getOrderById(Long orderId) {
        log.info("Getting order by orderId: {}", orderId);
        OrderVO result = OrderVO.builder().orderId(orderId).build();
        LogUtils.info(log, "Fetched order", result);
        return result;
    }

    @Override
    public List<OrderVO> getUserOrders(Long userId, int page, int size) {
        log.info("Getting orders for userId: {}, page: {}, size: {}", userId, page, size);
        List<OrderVO> result = List.of();
        LogUtils.info(log, "Fetched user orders", result);
        return result;
    }

    @Override
    public OrderVO updateOrderStatus(Long orderId, String status) {
        log.info("Updating orderId: {} status to: {}", orderId, status);
        OrderVO result = OrderVO.builder().orderId(orderId).orderStatus(status).build();
        LogUtils.info(log, "Order status updated", result);
        return result;
    }

    @Override
    public void cancelOrder(Long orderId) {
        log.info("Cancelling orderId: {}", orderId);
    }
}
