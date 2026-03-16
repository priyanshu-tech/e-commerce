package com.example.demo.vo.user;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * User Value Object
 * Represents user information transferred between microservices
 */
@Data
@Builder
public class UserVO {
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String status;
    private LocalDateTime createdAt;
}
