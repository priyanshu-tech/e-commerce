package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ORDERS")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "orders_seq")
    @SequenceGenerator(name = "orders_seq", sequenceName = "ORDERS_SEQ", allocationSize = 1)
    private Long orderId;

    @Column(nullable = false, unique = true)
    private String orderNumber;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private String orderStatus;

    @Column(nullable = false, updatable = false)
    private LocalDateTime orderDate;

    // Address snapshot
    @Column(nullable = false)
    private String shippingAddressLine1;

    private String shippingAddressLine2;

    @Column(nullable = false)
    private String shippingCity;

    @Column(nullable = false)
    private String shippingState;

    @Column(nullable = false)
    private String shippingZipCode;

    @Column(nullable = false)
    private String shippingCountry;

    @PrePersist
    protected void onCreate() {
        orderDate = LocalDateTime.now();
        if (orderStatus == null) orderStatus = "CONFIRMED";
    }
}
