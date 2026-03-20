package com.example.demo.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
@Tag("integration")
class UserIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    // ── Happy Journeys ──────────────────────────────────────────────────────

    @Test
    void createUser_success() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "john_doe",
                                "email", "john@example.com",
                                "firstName", "John",
                                "lastName", "Doe",
                                "phoneNumber", "+911234567890"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getUser_success() throws Exception {
        // create first
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "get_user_test",
                                "email", "getuser@example.com",
                                "firstName", "Get",
                                "lastName", "User"
                        ))))
                .andExpect(status().isCreated());

        // then fetch
        mockMvc.perform(get("/api/users")
                        .param("username", "get_user_test")
                        .param("email", "getuser@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("get_user_test"))
                .andExpect(jsonPath("$.email").value("getuser@example.com"));
    }

    @Test
    void updateUser_success() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "update_user_test",
                                "email", "updateuser@example.com",
                                "firstName", "Old",
                                "lastName", "Name"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/users/update")
                        .param("username", "update_user_test")
                        .param("email", "updateuser@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "firstName", "New",
                                "lastName", "Name",
                                "phoneNumber", "+919999999999",
                                "status", "ACTIVE"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("New"))
                .andExpect(jsonPath("$.phoneNumber").value("+919999999999"));
    }

    @Test
    void addAddress_and_getAddresses_success() throws Exception {
        // create user
        String response = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "addr_user_test",
                                "email", "addruser@example.com",
                                "firstName", "Addr",
                                "lastName", "User"
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long userId = objectMapper.readTree(response).get("userId").asLong();

        // add address
        mockMvc.perform(post("/api/users/{userId}/addresses", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "addressLine1", "123 Main St",
                                "city", "Mumbai",
                                "state", "Maharashtra",
                                "zipCode", "400001",
                                "country", "India",
                                "addressType", "SHIPPING",
                                "isDefault", true
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.addressId").isNumber())
                .andExpect(jsonPath("$.city").value("Mumbai"));

        // get addresses
        mockMvc.perform(get("/api/users/{userId}/addresses", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].city").value("Mumbai"));
    }

    @Test
    void addSecondDefaultAddress_unsetsFirstDefault() throws Exception {
        String response = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "default_addr_test",
                                "email", "defaultaddr@example.com",
                                "firstName", "Default",
                                "lastName", "Test"
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long userId = objectMapper.readTree(response).get("userId").asLong();

        mockMvc.perform(post("/api/users/{userId}/addresses", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "addressLine1", "First Address",
                                "city", "Delhi",
                                "state", "Delhi",
                                "zipCode", "110001",
                                "country", "India",
                                "addressType", "SHIPPING",
                                "isDefault", true
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/users/{userId}/addresses", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "addressLine1", "Second Address",
                                "city", "Pune",
                                "state", "Maharashtra",
                                "zipCode", "411001",
                                "country", "India",
                                "addressType", "SHIPPING",
                                "isDefault", true
                        ))))
                .andExpect(status().isCreated());

        String addrRes = mockMvc.perform(get("/api/users/{userId}/addresses", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andReturn().getResponse().getContentAsString();

        // exactly one default
        long defaultCount = objectMapper.readTree(addrRes).findValues("isDefault")
                .stream().filter(n -> n.asBoolean()).count();
        org.junit.jupiter.api.Assertions.assertEquals(1, defaultCount);

        // the Pune address is the default
        var nodes = objectMapper.readTree(addrRes);
        for (var node : nodes) {
            if ("Pune".equals(node.get("city").asText())) {
                org.junit.jupiter.api.Assertions.assertTrue(node.get("isDefault").asBoolean());
            }
        }
    }

    // ── Failure Journeys ────────────────────────────────────────────────────

    @Test
    void createUser_duplicateUsername_returns409() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "dup_username",
                                "email", "dup1@example.com",
                                "firstName", "A",
                                "lastName", "B"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "dup_username",
                                "email", "dup2@example.com",
                                "firstName", "A",
                                "lastName", "B"
                        ))))
                .andExpect(status().isConflict());
    }

    @Test
    void createUser_duplicateEmail_returns409() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "dup_email1",
                                "email", "dupemail@example.com",
                                "firstName", "A",
                                "lastName", "B"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "dup_email2",
                                "email", "dupemail@example.com",
                                "firstName", "A",
                                "lastName", "B"
                        ))))
                .andExpect(status().isConflict());
    }

    @Test
    void getUser_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("username", "ghost_user")
                        .param("email", "ghost@example.com"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addAddress_userNotFound_returns404() throws Exception {
        mockMvc.perform(post("/api/users/{userId}/addresses", 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "addressLine1", "123 Ghost St",
                                "city", "Nowhere",
                                "state", "NA",
                                "zipCode", "000000",
                                "country", "India",
                                "addressType", "SHIPPING",
                                "isDefault", false
                        ))))
                .andExpect(status().isNotFound());
    }
}
