package com.example.demo.controller;

import com.example.demo.service.ProductService;
import com.example.demo.vo.product.CategoryVO;
import com.example.demo.vo.product.ProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    @Autowired
    private final ProductService productService;

    @GetMapping
    public List<ProductVO> getAllProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return productService.getAllProducts(category, search, page, size);
    }

    @GetMapping("/{productId}")
    public ProductVO getProductById(@PathVariable Long productId) {
        return productService.getProductById(productId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductVO createProduct(@RequestBody ProductVO productVO) {
        return productService.createProduct(productVO);
    }

    @PostMapping("/{productId}/update")
    public ProductVO updateProduct(@PathVariable Long productId, @RequestBody ProductVO productVO) {
        return productService.updateProduct(productId, productVO);
    }

    @PostMapping("/{productId}/toggle-status")
    public Map<String, String> toggleStatus(@PathVariable Long productId) {
        return productService.toggleStatus(productId);
    }

    @PostMapping("/{productId}/rating")
    public ProductVO updateRating(@PathVariable Long productId,
                                  @RequestParam Double rating,
                                  @RequestParam Integer reviewCount) {
        return productService.updateRating(productId, rating, reviewCount);
    }

    @GetMapping("/categories")
    public List<CategoryVO> getAllCategories() {
        return productService.getAllCategories();
    }
}
