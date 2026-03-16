package com.example.demo.service;

import com.example.demo.vo.product.CategoryVO;
import com.example.demo.vo.product.ProductVO;

import java.util.List;

/**
 * Product Service Interface
 * Handles product catalog and category business logic
 */
public interface ProductService {

    List<ProductVO> getAllProducts(String category, String search, int page, int size);

    ProductVO getProductById(Long productId);

    ProductVO createProduct(ProductVO productVO);

    ProductVO updateProduct(Long productId, ProductVO productVO);

    void deleteProduct(Long productId);

    List<CategoryVO> getAllCategories();
}
