package com.example.demo.service;

import com.example.demo.entity.Address;
import com.example.demo.entity.User;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.AddressRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.impl.UserServiceImpl;
import com.example.demo.vo.user.AddressVO;
import com.example.demo.vo.user.UserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock AddressRepository addressRepository;
    @InjectMocks UserServiceImpl userService;

    private User user;
    private UserVO userVO;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .userId(1L).username("raj").email("raj@test.com")
                .firstName("Raj").lastName("Kumar").phoneNumber("9999")
                .status("ACTIVE").createdAt(LocalDateTime.now()).build();

        userVO = UserVO.builder()
                .username("raj").email("raj@test.com")
                .firstName("Raj").lastName("Kumar").phoneNumber("9999")
                .status("ACTIVE").build();
    }

    @Test
    void getUser_success() {
        when(userRepository.findByUsernameAndEmail("raj", "raj@test.com")).thenReturn(Optional.of(user));
        UserVO result = userService.getUser("raj", "raj@test.com");
        assertThat(result.getUsername()).isEqualTo("raj");
    }

    @Test
    void getUser_notFound() {
        when(userRepository.findByUsernameAndEmail(any(), any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getUser("x", "x@test.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createUser_success() {
        when(userRepository.existsByUsername("raj")).thenReturn(false);
        when(userRepository.existsByEmail("raj@test.com")).thenReturn(false);
        when(userRepository.save(any())).thenReturn(user);
        UserVO result = userService.createUser(userVO);
        assertThat(result.getUsername()).isEqualTo("raj");
    }

    @Test
    void createUser_duplicateUsername() {
        when(userRepository.existsByUsername("raj")).thenReturn(true);
        assertThatThrownBy(() -> userService.createUser(userVO))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void createUser_duplicateEmail() {
        when(userRepository.existsByUsername("raj")).thenReturn(false);
        when(userRepository.existsByEmail("raj@test.com")).thenReturn(true);
        assertThatThrownBy(() -> userService.createUser(userVO))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void updateUser_success() {
        when(userRepository.findByUsernameAndEmail("raj", "raj@test.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);
        UserVO result = userService.updateUser("raj", "raj@test.com", userVO);
        assertThat(result).isNotNull();
    }

    @Test
    void updateUser_notFound() {
        when(userRepository.findByUsernameAndEmail(any(), any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.updateUser("x", "x@test.com", userVO))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getUserAddresses_success() {
        Address address = Address.builder().addressId(1L).userId(1L)
                .addressLine1("123 St").city("Mumbai").state("MH")
                .zipCode("400001").country("India").addressType("SHIPPING").isDefault(true).build();
        when(userRepository.existsById(1L)).thenReturn(true);
        when(addressRepository.findByUserId(1L)).thenReturn(List.of(address));
        List<AddressVO> result = userService.getUserAddresses(1L);
        assertThat(result).hasSize(1);
    }

    @Test
    void getUserAddresses_userNotFound() {
        when(userRepository.existsById(1L)).thenReturn(false);
        assertThatThrownBy(() -> userService.getUserAddresses(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addAddress_success_withDefault() {
        Address existing = Address.builder().addressId(2L).userId(1L).isDefault(true).build();
        Address saved = Address.builder().addressId(3L).userId(1L)
                .addressLine1("456 St").city("Delhi").state("DL")
                .zipCode("110001").country("India").addressType("BILLING").isDefault(true).build();

        when(userRepository.existsById(1L)).thenReturn(true);
        when(addressRepository.findByUserIdAndIsDefaultTrue(1L)).thenReturn(Optional.of(existing));
        when(addressRepository.save(any())).thenReturn(saved);

        AddressVO vo = AddressVO.builder().addressLine1("456 St").city("Delhi").state("DL")
                .zipCode("110001").country("India").addressType("BILLING").isDefault(true).build();
        AddressVO result = userService.addAddress(1L, vo);
        assertThat(result).isNotNull();
    }

    @Test
    void addAddress_userNotFound() {
        when(userRepository.existsById(1L)).thenReturn(false);
        assertThatThrownBy(() -> userService.addAddress(1L, AddressVO.builder().isDefault(false).build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addAddress_noExistingDefault() {
        Address saved = Address.builder().addressId(1L).userId(1L)
                .addressLine1("St").city("C").state("S").zipCode("Z").country("IN")
                .addressType("SHIPPING").isDefault(false).build();
        when(userRepository.existsById(1L)).thenReturn(true);
        when(addressRepository.save(any())).thenReturn(saved);

        AddressVO vo = AddressVO.builder().addressLine1("St").city("C").state("S")
                .zipCode("Z").country("IN").addressType("SHIPPING").isDefault(false).build();
        AddressVO result = userService.addAddress(1L, vo);
        assertThat(result).isNotNull();
    }
}
