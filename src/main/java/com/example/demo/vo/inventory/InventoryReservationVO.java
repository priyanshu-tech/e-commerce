package com.example.demo.vo.inventory;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InventoryReservationVO {
    private Long reservationId;
    private Long inventoryId;
    private Long orderId;
    private Integer quantity;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
