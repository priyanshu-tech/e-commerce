package com.example.demo.service.impl;

import com.example.demo.entity.Address;
import com.example.demo.entity.User;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.AddressRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.vo.user.AddressVO;
import com.example.demo.vo.user.UserVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock AddressRepository addressRepository;
    @InjectMocks UserServiceImpl userService;

    // ── createUser ──────────────────────────────────────────

    @Test
    void createUser_success() {
        UserVO vo = UserVO.builder().username("john").email("john@test.com").build();
        User saved = User.builder().userId(1L).username("john").email("john@test.com").build();

        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@test.com")).thenReturn(false);
        when(userRepository.save(any())).thenReturn(saved);

        UserVO result = userService.createUser(vo);

        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("john");
    }

    @Test
    void createUser_duplicateUsername_throws() {
        UserVO vo = UserVO.builder().username("john").email("john@test.com").build();
        when(userRepository.existsByUsername("john")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(vo))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("john");
    }

    @Test
    void createUser_duplicateEmail_throws() {
        UserVO vo = UserVO.builder().username("john").email("john@test.com").build();
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(vo))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("john@test.com");
    }

    // ── getUser ─────────────────────────────────────────────

    @Test
    void getUser_success() {
        User user = User.builder().userId(1L).username("john").email("john@test.com").build();
        when(userRepository.findByUsernameAndEmail("john", "john@test.com")).thenReturn(Optional.of(user));

        UserVO result = userService.getUser("john", "john@test.com");

        assertThat(result.getUsername()).isEqualTo("john");
    }

    @Test
    void getUser_notFound_throws() {
        when(userRepository.findByUsernameAndEmail("x", "x@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser("x", "x@test.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── addAddress ───────────────────────────────────────────

    @Test
    void addAddress_success() {
        AddressVO vo = AddressVO.builder().userId(1L).addressLine1("123 Main St")
                .city("Mumbai").state("MH").zipCode("400001").country("India")
                .addressType("HOME").isDefault(false).build();
        Address saved = Address.builder().addressId(1L).userId(1L).addressLine1("123 Main St")
                .city("Mumbai").state("MH").zipCode("400001").country("India")
                .addressType("HOME").isDefault(false).build();

        when(userRepository.existsById(1L)).thenReturn(true);
        when(addressRepository.save(any())).thenReturn(saved);

        AddressVO result = userService.addAddress(1L, vo);

        assertThat(result.getAddressId()).isEqualTo(1L);
        assertThat(result.getCity()).isEqualTo("Mumbai");
    }

    @Test
    void addAddress_userNotFound_throws() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> userService.addAddress(99L, AddressVO.builder().build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addAddress_newDefault_unsetsExisting() {
        AddressVO vo = AddressVO.builder().userId(1L).addressLine1("456 Park Ave")
                .city("Delhi").state("DL").zipCode("110001").country("India")
                .addressType("HOME").isDefault(true).build();
        Address existingDefault = Address.builder().addressId(2L).userId(1L).isDefault(true).build();
        Address saved = Address.builder().addressId(3L).userId(1L).isDefault(true).build();

        when(userRepository.existsById(1L)).thenReturn(true);
        when(addressRepository.findByUserIdAndIsDefaultTrue(1L)).thenReturn(Optional.of(existingDefault));
        when(addressRepository.save(any())).thenReturn(saved);

        userService.addAddress(1L, vo);

        verify(addressRepository, times(2)).save(any());
    }

    // ── getUserAddresses ─────────────────────────────────────

    @Test
    void getUserAddresses_success() {
        Address a = Address.builder().addressId(1L).userId(1L).addressLine1("123 Main St")
                .city("Mumbai").state("MH").zipCode("400001").country("India")
                .addressType("HOME").isDefault(false).build();
        when(userRepository.existsById(1L)).thenReturn(true);
        when(addressRepository.findByUserId(1L)).thenReturn(List.of(a));

        var result = userService.getUserAddresses(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void getUserAddresses_userNotFound_throws() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> userService.getUserAddresses(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
