package com.example.demo.service.impl;

import com.example.demo.config.DiscountConfig;
import com.example.demo.entity.*;
import com.example.demo.service.impl.*;
import com.example.demo.vo.cart.CartItemVO;
import com.example.demo.vo.cart.CartVO;
import com.example.demo.vo.inventory.InventoryReservationVO;
import com.example.demo.vo.inventory.InventoryVO;
import com.example.demo.vo.product.CategoryVO;
import com.example.demo.vo.product.ProductVO;
import com.example.demo.vo.user.AddressVO;
import com.example.demo.vo.user.UserVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MapperTest {

    // ── UserMapper ──────────────────────────────────────────────────────────

    @Test
    void userMapper_toVO_null() {
        assertThat(UserMapper.toVO(null)).isNull();
    }

    @Test
    void userMapper_toVO_success() {
        User user = User.builder().userId(1L).username("raj").email("raj@test.com")
                .firstName("Raj").lastName("Kumar").phoneNumber("9999")
                .status("ACTIVE").createdAt(LocalDateTime.now()).build();
        UserVO vo = UserMapper.toVO(user);
        assertThat(vo.getUsername()).isEqualTo("raj");
        assertThat(vo.getEmail()).isEqualTo("raj@test.com");
    }

    @Test
    void userMapper_toEntity_null() {
        assertThat(UserMapper.toEntity(null)).isNull();
    }

    @Test
    void userMapper_toEntity_success() {
        UserVO vo = UserVO.builder().username("raj").email("raj@test.com")
                .firstName("Raj").lastName("Kumar").status("ACTIVE").build();
        User user = UserMapper.toEntity(vo);
        assertThat(user.getUsername()).isEqualTo("raj");
    }

    @Test
    void userMapper_toAddressVO_null() {
        assertThat(UserMapper.toAddressVO(null)).isNull();
    }

    @Test
    void userMapper_toAddressVO_success() {
        Address address = Address.builder().addressId(1L).userId(1L)
                .addressLine1("123 St").city("Mumbai").state("MH")
                .zipCode("400001").country("India").addressType("SHIPPING").isDefault(true).build();
        AddressVO vo = UserMapper.toAddressVO(address);
        assertThat(vo.getCity()).isEqualTo("Mumbai");
        assertThat(vo.getIsDefault()).isTrue();
    }

    @Test
    void userMapper_toAddressEntity_null() {
        assertThat(UserMapper.toAddressEntity(null)).isNull();
    }

    @Test
    void userMapper_toAddressEntity_success() {
        AddressVO vo = AddressVO.builder().addressLine1("123 St").city("Mumbai")
                .state("MH").zipCode("400001").country("India")
                .addressType("SHIPPING").isDefault(false).build();
        Address address = UserMapper.toAddressEntity(vo);
        assertThat(address.getCity()).isEqualTo("Mumbai");
    }

    // ── ProductMapper ───────────────────────────────────────────────────────

    @Test
    void productMapper_toVO_noDiscount() {
        Product product = Product.builder().productId(1L).name("HP").sku("HP-001")
                .price(new BigDecimal("999.99")).brand("Sony").categoryId(1L)
                .status("ACTIVE").reviewCount(0).createdAt(LocalDateTime.now()).build();
        ProductVO vo = ProductMapper.toVO(product, List.of(), null, "Electronics");
        assertThat(vo.getDiscountPrice()).isNull();
        assertThat(vo.getCategoryName()).isEqualTo("Electronics");
    }

    @Test
    void productMapper_toVO_withDiscount() {
        Product product = Product.builder().productId(1L).name("HP").sku("HP-001")
                .price(new BigDecimal("1000.00")).brand("Sony").categoryId(1L)
                .status("ACTIVE").reviewCount(0).createdAt(LocalDateTime.now()).build();
        Discount discount = Discount.builder().discountId(1L).productId(1L)
                .discountPct(new BigDecimal("10")).build();
        ProductVO vo = ProductMapper.toVO(product, List.of(), discount, "Electronics");
        assertThat(vo.getDiscountPrice()).isEqualByComparingTo(new BigDecimal("900.00"));
    }

    @Test
    void productMapper_toEntity_success() {
        ProductVO vo = ProductVO.builder().name("HP").sku("HP-001")
                .price(new BigDecimal("999.99")).brand("Sony").categoryId(1L).status("ACTIVE").build();
        Product product = ProductMapper.toEntity(vo);
        assertThat(product.getSku()).isEqualTo("HP-001");
    }

    @Test
    void productMapper_toCategoryVO_success() {
        Category category = Category.builder().categoryId(1L).name("Electronics")
                .description("Gadgets").displayOrder(1).build();
        CategoryVO vo = ProductMapper.toCategoryVO(category);
        assertThat(vo.getName()).isEqualTo("Electronics");
    }

    @Test
    void productMapper_toImageVO_success() {
        ProductImage image = ProductImage.builder().imageId(1L).productId(1L)
                .imageUrl("http://img.com/1.jpg").displayOrder(1).isPrimary(true).build();
        assertThat(ProductMapper.toImageVO(image).getImageUrl()).isEqualTo("http://img.com/1.jpg");
    }

    @Test
    void productMapper_toImageEntity_success() {
        com.example.demo.vo.product.ProductImageVO vo = com.example.demo.vo.product.ProductImageVO.builder()
                .imageUrl("http://img.com/1.jpg").displayOrder(1).isPrimary(true).build();
        ProductImage image = ProductMapper.toImageEntity(vo, 1L);
        assertThat(image.getProductId()).isEqualTo(1L);
    }

    // ── InventoryMapper ─────────────────────────────────────────────────────

    @Test
    void inventoryMapper_toVO_notLowStock() {
        Inventory inventory = Inventory.builder().inventoryId(1L).productId(1L).sku("HP-001")
                .totalQuantity(100).availableQuantity(80).reservedQuantity(20)
                .minStockLevel(10).warehouseLocation("WH-A1").lastUpdated(LocalDateTime.now()).build();
        InventoryVO vo = InventoryMapper.toVO(inventory);
        assertThat(vo.getIsLowStock()).isFalse();
    }

    @Test
    void inventoryMapper_toVO_lowStock() {
        Inventory inventory = Inventory.builder().inventoryId(1L).productId(1L).sku("HP-001")
                .totalQuantity(100).availableQuantity(5).reservedQuantity(0)
                .minStockLevel(10).lastUpdated(LocalDateTime.now()).build();
        InventoryVO vo = InventoryMapper.toVO(inventory);
        assertThat(vo.getIsLowStock()).isTrue();
    }

    @Test
    void inventoryMapper_toReservationVO_success() {
        InventoryReservation reservation = InventoryReservation.builder()
                .reservationId(1L).inventoryId(1L).orderId(1L).quantity(5)
                .expiresAt(LocalDateTime.now().plusMinutes(15)).createdAt(LocalDateTime.now()).build();
        InventoryReservationVO vo = InventoryMapper.toReservationVO(reservation);
        assertThat(vo.getQuantity()).isEqualTo(5);
    }

    // ── CartMapper ──────────────────────────────────────────────────────────

    @Test
    void cartMapper_toItemVO_noDiscount() {
        CartItem cartItem = CartItem.builder().cartItemId(1L).cartId(1L).productId(1L).quantity(2).build();
        Product product = Product.builder().productId(1L).name("HP").sku("HP-001")
                .price(new BigDecimal("999.99")).build();
        DiscountConfig config = mock(DiscountConfig.class);

        CartItemVO vo = CartMapper.toItemVO(cartItem, product, null, config);
        assertThat(vo.getUnitPrice()).isEqualByComparingTo(new BigDecimal("999.99"));
        assertThat(vo.getTotalPrice()).isEqualByComparingTo(new BigDecimal("1999.98"));
    }

    @Test
    void cartMapper_toItemVO_withDiscount() {
        CartItem cartItem = CartItem.builder().cartItemId(1L).cartId(1L).productId(1L).quantity(1).build();
        Product product = Product.builder().productId(1L).name("HP").sku("HP-001")
                .price(new BigDecimal("1000.00")).build();
        Discount discount = Discount.builder().discountPct(new BigDecimal("10")).build();
        DiscountConfig config = mock(DiscountConfig.class);
        when(config.getMaxDiscountPct()).thenReturn(new BigDecimal("90"));

        CartItemVO vo = CartMapper.toItemVO(cartItem, product, discount, config);
        assertThat(vo.getUnitPrice()).isEqualByComparingTo(new BigDecimal("900.00"));
    }

    @Test
    void cartMapper_toCartVO_success() {
        CartItemVO item = CartItemVO.builder().cartItemId(1L).productId(1L).quantity(2)
                .totalPrice(new BigDecimal("1999.98")).build();
        CartVO vo = CartMapper.toCartVO(1L, 1L, List.of(item));
        assertThat(vo.getSubtotal()).isEqualByComparingTo(new BigDecimal("1999.98"));
        assertThat(vo.getTotalItems()).isEqualTo(2);
    }

    @Test
    void cartMapper_toCartVO_empty() {
        CartVO vo = CartMapper.toCartVO(1L, 1L, List.of());
        assertThat(vo.getSubtotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(vo.getTotalItems()).isEqualTo(0);
    }
}
