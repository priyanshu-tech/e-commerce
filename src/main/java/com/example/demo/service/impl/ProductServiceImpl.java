package com.example.demo.service.impl;

import com.example.demo.config.DiscountConfig;
import com.example.demo.entity.Category;
import com.example.demo.entity.Discount;
import com.example.demo.entity.Product;
import com.example.demo.entity.ProductImage;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.DiscountRepository;
import com.example.demo.repository.ProductImageRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.service.ProductService;
import com.example.demo.util.LogUtils;
import com.example.demo.vo.product.CategoryVO;
import com.example.demo.vo.product.ProductVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    @Autowired private final ProductRepository productRepository;
    @Autowired private final ProductImageRepository productImageRepository;
    @Autowired private final CategoryRepository categoryRepository;
    @Autowired private final DiscountRepository discountRepository;
    @Autowired private final DiscountConfig discountConfig;

    @Override
    @Transactional(readOnly = true)
    public List<ProductVO> getAllProducts(String categoryName, String search, int page, int size) {
        log.info("Getting all products - category: {}, search: {}, page: {}, size: {}", categoryName, search, page, size);

        Long categoryId = null;
        if (categoryName != null && !categoryName.isBlank()) {
            categoryId = categoryRepository.findByName(categoryName)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryName))
                    .getCategoryId();
        }

        String searchTerm = (search != null && search.isBlank()) ? null : search;
        List<Product> products = productRepository.findAllActive(categoryId, searchTerm, PageRequest.of(page, size)).getContent();

        List<ProductVO> result = products.stream()
                .map(p -> enrichProduct(p))
                .collect(Collectors.toList());
        LogUtils.info(log, "Fetched products count", result.size());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ProductVO getProductById(Long productId) {
        log.info("Getting product by productId: {}", productId);
        Product product = productRepository.findByProductIdAndStatus(productId, "ACTIVE")
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        ProductVO result = enrichProduct(product);
        LogUtils.info(log, "Fetched product", result);
        return result;
    }

    @Override
    @Transactional
    public ProductVO createProduct(ProductVO productVO) {
        LogUtils.info(log, "Creating product", productVO);
        if (productRepository.existsBySku(productVO.getSku())) {
            throw new DuplicateResourceException("Product already exists with SKU: " + productVO.getSku());
        }
        Product saved = productRepository.save(ProductMapper.toEntity(productVO));
        saveImages(productVO, saved.getProductId());
        ProductVO result = enrichProduct(saved);
        LogUtils.info(log, "Product created", result);
        return result;
    }

    @Override
    @Transactional
    public ProductVO updateProduct(Long productId, ProductVO productVO) {
        log.info("Updating productId: {}", productId);
        LogUtils.info(log, "Update payload", productVO);
        Product existing = productRepository.findByProductIdAndStatus(productId, "ACTIVE")
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        existing.setName(productVO.getName());
        existing.setDescription(productVO.getDescription());
        existing.setPrice(productVO.getPrice());
        existing.setBrand(productVO.getBrand());
        existing.setCategoryId(productVO.getCategoryId());
        Product updated = productRepository.save(existing);

        if (productVO.getImages() != null) {
            productImageRepository.deleteByProductId(productId);
            saveImages(productVO, productId);
        }

        ProductVO result = enrichProduct(updated);
        LogUtils.info(log, "Product updated", result);
        return result;
    }

    @Override
    @Transactional
    public Map<String, String> toggleStatus(Long productId) {
        log.info("Toggling status for productId: {}", productId);
        Product existing = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        String newStatus = "ACTIVE".equals(existing.getStatus()) ? "INACTIVE" : "ACTIVE";
        existing.setStatus(newStatus);
        productRepository.save(existing);
        log.info("Product {} status toggled to {}", productId, newStatus);
        return Map.of("message", "Product status changed to " + newStatus);
    }

    @Override
    @Transactional
    public ProductVO updateRating(Long productId, Double rating, Integer reviewCount) {
        log.info("Updating rating for productId: {}, rating: {}, reviewCount: {}", productId, rating, reviewCount);
        Product existing = productRepository.findByProductIdAndStatus(productId, "ACTIVE")
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        existing.setRating(rating);
        existing.setReviewCount(reviewCount);
        Product updated = productRepository.save(existing);
        ProductVO result = enrichProduct(updated);
        LogUtils.info(log, "Product rating updated", result);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryVO> getAllCategories() {
        log.info("Getting all categories");
        List<CategoryVO> result = categoryRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(ProductMapper::toCategoryVO)
                .collect(Collectors.toList());
        LogUtils.info(log, "Fetched categories count", result.size());
        return result;
    }

    private ProductVO enrichProduct(Product product) {
        List<ProductImage> images = productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getProductId());
        Discount discount = discountRepository.findActiveDiscount(product.getProductId(), LocalDateTime.now()).orElse(null);

        if (discount != null && discount.getDiscountPct().compareTo(discountConfig.getMaxDiscountPct()) > 0) {
            log.warn("Discount {}% on productId {} exceeds configured max {}% — capping",
                    discount.getDiscountPct(), product.getProductId(), discountConfig.getMaxDiscountPct());
            discount.setDiscountPct(discountConfig.getMaxDiscountPct());
        }

        String categoryName = categoryRepository.findById(product.getCategoryId())
                .map(Category::getName)
                .orElse(null);

        return ProductMapper.toVO(product, images, discount, categoryName);
    }

    private void saveImages(ProductVO productVO, Long productId) {
        if (productVO.getImages() == null || productVO.getImages().isEmpty()) return;
        productVO.getImages().forEach(imgVO ->
                productImageRepository.save(ProductMapper.toImageEntity(imgVO, productId)));
    }
}
