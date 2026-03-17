package com.example.demo.repository;

import com.example.demo.entity.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {

    Optional<InventoryReservation> findByOrderIdAndInventoryId(Long orderId, Long inventoryId);

    List<InventoryReservation> findByOrderId(Long orderId);

    @Query("SELECT r FROM InventoryReservation r WHERE r.expiresAt < :now")
    List<InventoryReservation> findExpiredReservations(@Param("now") LocalDateTime now);
}
