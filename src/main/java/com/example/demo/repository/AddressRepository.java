package com.example.demo.repository;

import com.example.demo.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByUsernameAndEmail(String username, String email);

    Optional<Address> findByUsernameAndEmailAndIsDefaultTrue(String username, String email);

    List<Address> findByUsernameAndEmailAndAddressType(String username, String email, String addressType);
}
