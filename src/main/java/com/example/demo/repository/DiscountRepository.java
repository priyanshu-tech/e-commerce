package com.example.demo.repository;

import com.example.demo.entity.Discount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, Long> {

    @Query("SELECT d FROM Discount d WHERE d.productId = :productId " +
           "AND :now BETWEEN d.startDate AND d.endDate")
    Optional<Discount> findActiveDiscount(@Param("productId") Long productId,
                                          @Param("now") LocalDateTime now);
}
