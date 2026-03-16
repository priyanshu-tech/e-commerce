package com.example.demo.service.impl;

import com.example.demo.service.ProductService;
import com.example.demo.util.LogUtils;
import com.example.demo.vo.product.CategoryVO;
import com.example.demo.vo.product.ProductVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    @Override
    public List<ProductVO> getAllProducts(String category, String search, int page, int size) {
        log.info("Getting all products - category: {}, search: {}, page: {}, size: {}", category, search, page, size);
        List<ProductVO> result = List.of();
        LogUtils.info(log, "Fetched products", result);
        return result;
    }

    @Override
    public ProductVO getProductById(Long productId) {
        log.info("Getting product by productId: {}", productId);
        ProductVO result = ProductVO.builder().productId(productId).build();
        LogUtils.info(log, "Fetched product", result);
        return result;
    }

    @Override
    public ProductVO createProduct(ProductVO productVO) {
        LogUtils.info(log, "Creating product", productVO);
        LogUtils.info(log, "Product created", productVO);
        return productVO;
    }

    @Override
    public ProductVO updateProduct(Long productId, ProductVO productVO) {
        log.info("Updating productId: {}", productId);
        LogUtils.info(log, "Update payload", productVO);
        LogUtils.info(log, "Product updated", productVO);
        return productVO;
    }

    @Override
    public void deleteProduct(Long productId) {
        log.info("Deleting productId: {}", productId);
    }

    @Override
    public List<CategoryVO> getAllCategories() {
        log.info("Getting all categories");
        List<CategoryVO> result = List.of();
        LogUtils.info(log, "Fetched categories", result);
        return result;
    }
}
