package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "DISCOUNTS")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Discount {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "discounts_seq")
    @SequenceGenerator(name = "discounts_seq", sequenceName = "DISCOUNTS_SEQ", allocationSize = 1)
    private Long discountId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPct;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;
}
