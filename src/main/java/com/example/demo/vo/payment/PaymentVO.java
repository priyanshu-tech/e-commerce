package com.example.demo.vo.payment;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment Value Object
 * Represents payment transaction information
 */
@Data
@Builder
public class PaymentVO {
    private Long paymentId;
    private Long orderId;
    private String paymentMethod;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String transactionId;
    private String gatewayResponse;
    private LocalDateTime paymentDate;
}
