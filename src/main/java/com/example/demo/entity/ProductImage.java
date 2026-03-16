package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "PRODUCT_IMAGES")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_images_seq")
    @SequenceGenerator(name = "product_images_seq", sequenceName = "PRODUCT_IMAGES_SEQ", allocationSize = 1)
    private Long imageId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private Integer displayOrder;

    @Column(nullable = false)
    private Boolean isPrimary;

    @PrePersist
    protected void onCreate() {
        if (isPrimary == null) isPrimary = false;
        if (displayOrder == null) displayOrder = 0;
    }
}
