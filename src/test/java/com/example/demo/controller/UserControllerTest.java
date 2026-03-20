package com.example.demo.controller;

import com.example.demo.exception.GlobalExceptionHandler;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.service.UserService;
import com.example.demo.vo.user.AddressVO;
import com.example.demo.vo.user.UserVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock UserService userService;
    @InjectMocks UserController userController;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    private UserVO userVO;
    private AddressVO addressVO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        userVO = UserVO.builder().userId(1L).username("raj").email("raj@test.com")
                .firstName("Raj").lastName("Kumar").status("ACTIVE").build();

        addressVO = AddressVO.builder().addressId(1L).userId(1L)
                .addressLine1("123 St").city("Mumbai").state("MH")
                .zipCode("400001").country("India").addressType("SHIPPING").isDefault(true).build();
    }

    @Test
    void getUser_success() throws Exception {
        when(userService.getUser("raj", "raj@test.com")).thenReturn(userVO);
        mockMvc.perform(get("/api/users").param("username", "raj").param("email", "raj@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("raj"));
    }

    @Test
    void getUser_notFound() throws Exception {
        when(userService.getUser(any(), any())).thenThrow(new ResourceNotFoundException("User not found"));
        mockMvc.perform(get("/api/users").param("username", "x").param("email", "x@test.com"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createUser_success() throws Exception {
        when(userService.createUser(any())).thenReturn(userVO);
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userVO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("raj"));
    }

    @Test
    void updateUser_success() throws Exception {
        when(userService.updateUser(eq("raj"), eq("raj@test.com"), any())).thenReturn(userVO);
        mockMvc.perform(post("/api/users/update")
                        .param("username", "raj").param("email", "raj@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("raj"));
    }

    @Test
    void getUserAddresses_success() throws Exception {
        when(userService.getUserAddresses(1L)).thenReturn(List.of(addressVO));
        mockMvc.perform(get("/api/users/1/addresses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].city").value("Mumbai"));
    }

    @Test
    void getUserAddresses_notFound() throws Exception {
        when(userService.getUserAddresses(1L)).thenThrow(new ResourceNotFoundException("User not found"));
        mockMvc.perform(get("/api/users/1/addresses"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addAddress_success() throws Exception {
        when(userService.addAddress(eq(1L), any())).thenReturn(addressVO);
        mockMvc.perform(post("/api/users/1/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addressVO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.city").value("Mumbai"));
    }
}
