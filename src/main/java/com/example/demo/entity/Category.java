package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "CATEGORIES")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "categories_seq")
    @SequenceGenerator(name = "categories_seq", sequenceName = "CATEGORIES_SEQ", allocationSize = 1)
    private Long categoryId;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    private Long parentCategoryId;

    private String imageUrl;

    @Column(nullable = false)
    private Integer displayOrder;
}
