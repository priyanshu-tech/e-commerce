package com.example.demo.controller;

import com.example.demo.service.OrderService;
import com.example.demo.vo.order.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderVO placeOrder(@RequestParam Long userId, @RequestParam Long addressId) {
        return orderService.placeOrder(userId, addressId);
    }

    @GetMapping("/{orderNumber}")
    public OrderVO getOrderByNumber(@PathVariable String orderNumber) {
        return orderService.getOrderByNumber(orderNumber);
    }

    @GetMapping("/user/{userId}")
    public List<OrderVO> getUserOrders(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return orderService.getUserOrders(userId, page, size);
    }

    @PostMapping("/{orderNumber}/cancel")
    public Map<String, String> cancelOrder(@PathVariable String orderNumber) {
        return orderService.cancelOrder(orderNumber);
    }
}
