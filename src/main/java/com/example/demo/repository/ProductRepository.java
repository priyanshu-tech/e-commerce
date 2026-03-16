package com.example.demo.repository;

import com.example.demo.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySku(String sku);

    Optional<Product> findByProductIdAndStatus(Long productId, String status);

    @Query("SELECT p FROM Product p WHERE p.status = 'ACTIVE' " +
           "AND (:categoryId IS NULL OR p.categoryId = :categoryId) " +
           "AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))")

    Page<Product> findAllActive(@Param("categoryId") Long categoryId,
                                @Param("search") String search,
                                Pageable pageable);
}
