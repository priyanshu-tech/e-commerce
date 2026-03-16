package com.example.demo.vo.order;

import com.example.demo.vo.user.AddressVO;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Order Value Object
 * Represents customer order information
 */
@Data
@Builder
public class OrderVO {
    private Long orderId;
    private String orderNumber;
    private Long userId;
    private List<OrderItemVO> items;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal shippingCost;
    private BigDecimal totalAmount;
    private String orderStatus;
    private String paymentStatus;
    private AddressVO shippingAddress;
    private AddressVO billingAddress;
    private LocalDateTime orderDate;
    private LocalDateTime estimatedDeliveryDate;
}
