package com.example.demo.service;

import com.example.demo.vo.product.CategoryVO;
import com.example.demo.vo.product.ProductVO;

import java.util.List;
import java.util.Map;

public interface ProductService {

    List<ProductVO> getAllProducts(String category, String search, int page, int size);

    ProductVO getProductById(Long productId);

    ProductVO createProduct(ProductVO productVO);

    ProductVO updateProduct(Long productId, ProductVO productVO);

    Map<String, String> toggleStatus(Long productId);

    ProductVO updateRating(Long productId, Double rating, Integer reviewCount);

    List<CategoryVO> getAllCategories();
}
