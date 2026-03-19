package com.example.demo.vo.payment;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PaymentVO {
    private Long paymentId;
    private Long orderId;
    private String orderNumber;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private Long amount;          // in paise (INR × 100)
    private String currency;
    private String status;        // PENDING | SUCCESS | FAILED | REFUNDED
    private LocalDateTime paymentDate;
}
