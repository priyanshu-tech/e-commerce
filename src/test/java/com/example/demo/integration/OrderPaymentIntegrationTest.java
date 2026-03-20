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
class OrderPaymentIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    /**
     * Full setup: user + address + product + inventory + cart item.
     * Returns [userId, addressId, productId, inventoryId]
     */
    private long[] setupOrderPrerequisites(String suffix) throws Exception {
        // 1. Create user
        String userRes = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "order_user_" + suffix,
                                "email", "orderuser_" + suffix + "@example.com",
                                "firstName", "Order",
                                "lastName", "User"
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long userId = objectMapper.readTree(userRes).get("userId").asLong();

        // 2. Add address
        String addrRes = mockMvc.perform(post("/api/users/{userId}/addresses", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "addressLine1", "101 Order Street",
                                "city", "Bangalore",
                                "state", "Karnataka",
                                "zipCode", "560001",
                                "country", "India",
                                "addressType", "SHIPPING",
                                "isDefault", true
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long addressId = objectMapper.readTree(addrRes).get("addressId").asLong();

        // 3. Create product
        String prodRes = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Order Product " + suffix,
                                "sku", "ORD-PROD-" + suffix,
                                "price", 5000,
                                "brand", "Brand",
                                "categoryId", 1
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long productId = objectMapper.readTree(prodRes).get("productId").asLong();

        // 4. Create inventory
        String invRes = mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sku", "ORD-PROD-" + suffix,
                                "totalQuantity", 100,
                                "availableQuantity", 100,
                                "reservedQuantity", 0,
                                "minStockLevel", 5
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long inventoryId = objectMapper.readTree(invRes).get("inventoryId").asLong();

        // 5. Add to cart
        mockMvc.perform(post("/api/cart/{userId}/items", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("productId", productId, "quantity", 2))))
                .andExpect(status().isCreated());

        return new long[]{userId, addressId, productId, inventoryId};
    }

    // ── Happy Journeys ──────────────────────────────────────────────────────

    @Test
    void placeOrder_success() throws Exception {
        long[] ids = setupOrderPrerequisites("001");
        long userId = ids[0], addressId = ids[1];

        mockMvc.perform(post("/api/orders")
                        .param("userId", String.valueOf(userId))
                        .param("addressId", String.valueOf(addressId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").value(startsWith("ORD-")))
                .andExpect(jsonPath("$.orderStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.totalAmount").value(10000.00))
                .andExpect(jsonPath("$.shippingCity").value("Bangalore"));
    }

    @Test
    void placeOrder_cartClearedAfterOrder() throws Exception {
        long[] ids = setupOrderPrerequisites("002");
        long userId = ids[0], addressId = ids[1];

        mockMvc.perform(post("/api/orders")
                        .param("userId", String.valueOf(userId))
                        .param("addressId", String.valueOf(addressId)))
                .andExpect(status().isCreated());

        // cart should be empty after order
        mockMvc.perform(get("/api/cart/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void placeOrder_inventoryReservedAfterOrder() throws Exception {
        long[] ids = setupOrderPrerequisites("003");
        long userId = ids[0], addressId = ids[1], productId = ids[2];

        mockMvc.perform(post("/api/orders")
                        .param("userId", String.valueOf(userId))
                        .param("addressId", String.valueOf(addressId)))
                .andExpect(status().isCreated());

        // available should decrease by 2
        mockMvc.perform(get("/api/inventory/product/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(98))
                .andExpect(jsonPath("$.reservedQuantity").value(2));
    }

    @Test
    void getOrderByNumber_success() throws Exception {
        long[] ids = setupOrderPrerequisites("004");

        String res = mockMvc.perform(post("/api/orders")
                        .param("userId", String.valueOf(ids[0]))
                        .param("addressId", String.valueOf(ids[1])))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String orderNumber = objectMapper.readTree(res).get("orderNumber").asText();

        mockMvc.perform(get("/api/orders/{orderNumber}", orderNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value(orderNumber))
                .andExpect(jsonPath("$.orderStatus").value("CONFIRMED"));
    }

    @Test
    void getUserOrders_success() throws Exception {
        long[] ids = setupOrderPrerequisites("005");
        long userId = ids[0];

        mockMvc.perform(post("/api/orders")
                        .param("userId", String.valueOf(userId))
                        .param("addressId", String.valueOf(ids[1])))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/orders/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void cancelOrder_success() throws Exception {
        long[] ids = setupOrderPrerequisites("006");

        String res = mockMvc.perform(post("/api/orders")
                        .param("userId", String.valueOf(ids[0]))
                        .param("addressId", String.valueOf(ids[1])))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String orderNumber = objectMapper.readTree(res).get("orderNumber").asText();

        mockMvc.perform(post("/api/orders/{orderNumber}/cancel", orderNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        // order status should be CANCELLED
        mockMvc.perform(get("/api/orders/{orderNumber}", orderNumber))
                .andExpect(jsonPath("$.orderStatus").value("CANCELLED"));
    }

    @Test
    void cancelOrder_inventoryReleasedAfterCancel() throws Exception {
        long[] ids = setupOrderPrerequisites("007");
        long productId = ids[2];

        String res = mockMvc.perform(post("/api/orders")
                        .param("userId", String.valueOf(ids[0]))
                        .param("addressId", String.valueOf(ids[1])))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String orderNumber = objectMapper.readTree(res).get("orderNumber").asText();

        mockMvc.perform(post("/api/orders/{orderNumber}/cancel", orderNumber))
                .andExpect(status().isOk());

        // inventory should be restored
        mockMvc.perform(get("/api/inventory/product/{productId}", productId))
                .andExpect(jsonPath("$.availableQuantity").value(100))
                .andExpect(jsonPath("$.reservedQuantity").value(0));
    }

    @Test
    void fullPaymentFlow_success() throws Exception {
        long[] ids = setupOrderPrerequisites("008");

        String orderRes = mockMvc.perform(post("/api/orders")
                        .param("userId", String.valueOf(ids[0]))
                        .param("addressId", String.valueOf(ids[1])))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String orderNumber = objectMapper.readTree(orderRes).get("orderNumber").asText();

        // create payment
        String payRes = mockMvc.perform(post("/api/payments/create-order")
                        .param("orderNumber", orderNumber)
                        .param("currency", "INR"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.razorpayOrderId").value(startsWith("mock_order_")))
                .andReturn().getResponse().getContentAsString();

        String razorpayOrderId = objectMapper.readTree(payRes).get("razorpayOrderId").asText();

        // verify with valid signature
        mockMvc.perform(post("/api/payments/verify")
                        .param("razorpayOrderId", razorpayOrderId)
                        .param("razorpayPaymentId", "pay_test_123")
                        .param("razorpaySignature", "valid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.razorpayPaymentId").value("pay_test_123"));

        // order status should be PAID
        mockMvc.perform(get("/api/orders/{orderNumber}", orderNumber))
                .andExpect(jsonPath("$.orderStatus").value("PAID"));
    }

    @Test
    void getPaymentByOrderNumber_success() throws Exception {
        long[] ids = setupOrderPrerequisites("009");

        String orderRes = mockMvc.perform(post("/api/orders")
                        .param("userId", String.valueOf(ids[0]))
                        .param("addressId", String.valueOf(ids[1])))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String orderNumber = objectMapper.readTree(orderRes).get("orderNumber").asText();

        mockMvc.perform(post("/api/payments/create-order")
                        .param("orderNumber", orderNumber))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/payments/{orderNumber}", orderNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value(orderNumber))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void refundPayment_success() throws Exception {
        long[] ids = setupOrderPrerequisites("010");

        String orderRes = mockMvc.perform(post("/api/orders")
                        .param("userId", String.valueOf(ids[0]))
                        .param("addressId", String.valueOf(ids[1])))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String orderNumber = objectMapper.readTree(orderRes).get("orderNumber").asText();

        String payRes = mockMvc.perform(post("/api/payments/create-order")
                        .param("orderNumber", orderNumber))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String razorpayOrderId = objectMapper.readTree(payRes).get("razorpayOrderId").asText();

        // verify → SUCCESS
        mockMvc.perform(post("/api/payments/verify")
                        .param("razorpayOrderId", razorpayOrderId)
                        .param("razorpayPaymentId", "pay_refund_test")
                        .param("razorpaySignature", "valid"))
                .andExpect(status().isOk());

        // refund
        mockMvc.perform(post("/api/payments/{orderNumber}/refund", orderNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        // payment status should be REFUNDED
        mockMvc.perform(get("/api/payments/{orderNumber}", orderNumber))
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }

    // ── Failure Journeys ────────────────────────────────────────────────────

    @Test
    void placeOrder_emptyCart_returns500() throws Exception {
        String userRes = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "empty_cart_user",
                                "email", "emptycart@example.com",
                                "firstName", "Empty",
                                "lastName", "Cart"
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long userId = objectMapper.readTree(userRes).get("userId").asLong();

        String addrRes = mockMvc.perform(post("/api/users/{userId}/addresses", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "addressLine1", "Ghost St",
                                "city", "Delhi",
                                "state", "Delhi",
                                "zipCode", "110001",
                                "country", "India",
                                "addressType", "SHIPPING",
                                "isDefault", true
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long addressId = objectMapper.readTree(addrRes).get("addressId").asLong();

        // trigger cart creation first (getCart auto-creates)
        mockMvc.perform(get("/api/cart/{userId}", userId))
                .andExpect(status().isOk());

        // now place order with empty cart → 500
        mockMvc.perform(post("/api/orders")
                        .param("userId", String.valueOf(userId))
                        .param("addressId", String.valueOf(addressId)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void placeOrder_addressNotFound_returns404() throws Exception {
        long[] ids = setupOrderPrerequisites("011");

        mockMvc.perform(post("/api/orders")
                        .param("userId", String.valueOf(ids[0]))
                        .param("addressId", "99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getOrderByNumber_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/orders/{orderNumber}", "ORD-GHOST-0000"))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelOrder_alreadyCancelled_returns500() throws Exception {
        long[] ids = setupOrderPrerequisites("012");

        String res = mockMvc.perform(post("/api/orders")
                        .param("userId", String.valueOf(ids[0]))
                        .param("addressId", String.valueOf(ids[1])))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String orderNumber = objectMapper.readTree(res).get("orderNumber").asText();

        mockMvc.perform(post("/api/orders/{orderNumber}/cancel", orderNumber))
                .andExpect(status().isOk());

        // cancel again → should fail
        mockMvc.perform(post("/api/orders/{orderNumber}/cancel", orderNumber))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void createPayment_orderNotFound_returns404() throws Exception {
        mockMvc.perform(post("/api/payments/create-order")
                        .param("orderNumber", "ORD-GHOST-9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createPayment_duplicate_returns500() throws Exception {
        long[] ids = setupOrderPrerequisites("013");

        String orderRes = mockMvc.perform(post("/api/orders")
                        .param("userId", String.valueOf(ids[0]))
                        .param("addressId", String.valueOf(ids[1])))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String orderNumber = objectMapper.readTree(orderRes).get("orderNumber").asText();

        mockMvc.perform(post("/api/payments/create-order")
                        .param("orderNumber", orderNumber))
                .andExpect(status().isCreated());

        // second payment for same order → should fail
        mockMvc.perform(post("/api/payments/create-order")
                        .param("orderNumber", orderNumber))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void verifyPayment_invalidSignature_statusFailed() throws Exception {
        long[] ids = setupOrderPrerequisites("014");

        String orderRes = mockMvc.perform(post("/api/orders")
                        .param("userId", String.valueOf(ids[0]))
                        .param("addressId", String.valueOf(ids[1])))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String orderNumber = objectMapper.readTree(orderRes).get("orderNumber").asText();

        String payRes = mockMvc.perform(post("/api/payments/create-order")
                        .param("orderNumber", orderNumber))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String razorpayOrderId = objectMapper.readTree(payRes).get("razorpayOrderId").asText();

        mockMvc.perform(post("/api/payments/verify")
                        .param("razorpayOrderId", razorpayOrderId)
                        .param("razorpayPaymentId", "pay_invalid")
                        .param("razorpaySignature", "invalid_sig"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void refundPayment_notSuccess_returns500() throws Exception {
        long[] ids = setupOrderPrerequisites("015");

        String orderRes = mockMvc.perform(post("/api/orders")
                        .param("userId", String.valueOf(ids[0]))
                        .param("addressId", String.valueOf(ids[1])))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String orderNumber = objectMapper.readTree(orderRes).get("orderNumber").asText();

        // create payment but don't verify (status = PENDING)
        mockMvc.perform(post("/api/payments/create-order")
                        .param("orderNumber", orderNumber))
                .andExpect(status().isCreated());

        // refund PENDING payment → should fail
        mockMvc.perform(post("/api/payments/{orderNumber}/refund", orderNumber))
                .andExpect(status().isInternalServerError());
    }
}
