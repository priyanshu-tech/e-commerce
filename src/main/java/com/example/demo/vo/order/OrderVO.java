package com.example.demo.vo.order;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderVO {
    private Long orderId;
    private String orderNumber;
    private Long userId;
    private List<OrderItemVO> items;
    private BigDecimal totalAmount;
    private String orderStatus;
    private LocalDateTime orderDate;

    // Address snapshot
    private String shippingAddressLine1;
    private String shippingAddressLine2;
    private String shippingCity;
    private String shippingState;
    private String shippingZipCode;
    private String shippingCountry;
}
