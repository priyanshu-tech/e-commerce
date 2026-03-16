package com.example.demo.repository;

import com.example.demo.entity.User;
import com.example.demo.entity.UserId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, UserId> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameAndEmail(String username, String email);

    boolean existsByUsernameAndEmail(String username, String email);

    boolean existsByEmail(String email);
}
