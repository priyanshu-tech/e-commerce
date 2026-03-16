package com.example.demo.service.impl;

import com.example.demo.entity.Category;
import com.example.demo.entity.Discount;
import com.example.demo.entity.Product;
import com.example.demo.entity.ProductImage;
import com.example.demo.util.LogUtils;
import com.example.demo.vo.product.CategoryVO;
import com.example.demo.vo.product.ProductImageVO;
import com.example.demo.vo.product.ProductVO;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
class ProductMapper {

    static ProductVO toVO(Product product, List<ProductImage> images, Discount discount, String categoryName) {
        List<ProductImageVO> imageVOs = images.stream()
                .map(ProductMapper::toImageVO)
                .collect(Collectors.toList());

        BigDecimal discountPrice = null;
        if (discount != null) {
            BigDecimal multiplier = BigDecimal.ONE.subtract(
                    discount.getDiscountPct().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
            discountPrice = product.getPrice().multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
        }

        ProductVO result = ProductVO.builder()
                .productId(product.getProductId())
                .name(product.getName())
                .description(product.getDescription())
                .sku(product.getSku())
                .price(product.getPrice())
                .discountPrice(discountPrice)
                .brand(product.getBrand())
                .categoryId(product.getCategoryId())
                .categoryName(categoryName)
                .images(imageVOs)
                .rating(product.getRating())
                .reviewCount(product.getReviewCount())
                .status(product.getStatus())
                .createdAt(product.getCreatedAt())
                .build();
        LogUtils.info(log, "Mapped Product to ProductVO", result);
        return result;
    }

    static Product toEntity(ProductVO vo) {
        LogUtils.info(log, "Mapping ProductVO to entity", vo);
        return Product.builder()
                .name(vo.getName())
                .description(vo.getDescription())
                .sku(vo.getSku())
                .price(vo.getPrice())
                .brand(vo.getBrand())
                .categoryId(vo.getCategoryId())
                .status(vo.getStatus())
                .build();
    }

    static ProductImageVO toImageVO(ProductImage image) {
        return ProductImageVO.builder()
                .imageId(image.getImageId())
                .productId(image.getProductId())
                .imageUrl(image.getImageUrl())
                .displayOrder(image.getDisplayOrder())
                .isPrimary(image.getIsPrimary())
                .build();
    }

    static ProductImage toImageEntity(ProductImageVO vo, Long productId) {
        return ProductImage.builder()
                .productId(productId)
                .imageUrl(vo.getImageUrl())
                .displayOrder(vo.getDisplayOrder())
                .isPrimary(vo.getIsPrimary())
                .build();
    }

    static CategoryVO toCategoryVO(Category category) {
        CategoryVO result = CategoryVO.builder()
                .categoryId(category.getCategoryId())
                .name(category.getName())
                .description(category.getDescription())
                .parentCategoryId(category.getParentCategoryId())
                .imageUrl(category.getImageUrl())
                .displayOrder(category.getDisplayOrder())
                .build();
        LogUtils.info(log, "Mapped Category to CategoryVO", result);
        return result;
    }
}
