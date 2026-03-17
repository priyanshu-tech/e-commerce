package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "INVENTORY_RESERVATIONS")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inventory_reservations_seq")
    @SequenceGenerator(name = "inventory_reservations_seq", sequenceName = "INVENTORY_RESERVATIONS_SEQ", allocationSize = 1)
    private Long reservationId;

    @Column(nullable = false)
    private Long inventoryId;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
