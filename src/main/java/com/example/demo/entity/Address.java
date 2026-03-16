package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ADDRESSES")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "addresses_seq")
    @SequenceGenerator(name = "addresses_seq", sequenceName = "ADDRESSES_SEQ", allocationSize = 1)
    private Long addressId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String addressLine1;

    private String addressLine2;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String zipCode;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    private String addressType;

    @Column(nullable = false)
    private Boolean isDefault;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "username", referencedColumnName = "username", insertable = false, updatable = false),
            @JoinColumn(name = "email", referencedColumnName = "email", insertable = false, updatable = false)
    })
    private User user;

    @PrePersist
    protected void onCreate() {
        if (isDefault == null) {
            isDefault = false;
        }
    }
}
