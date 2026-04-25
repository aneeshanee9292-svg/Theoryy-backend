package com.zym.ecart.controller;

import com.zym.ecart.dto.ApiResponse;
import com.zym.ecart.entity.Product;
import com.zym.ecart.repository.ProductRepository;
import com.zym.ecart.service.ProductService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final ProductRepository productRepository;

   

    public ProductController(ProductService productService, ProductRepository productRepository) {
		super();
		this.productService = productService;
		this.productRepository = productRepository;
	}

	@PostMapping
    public ResponseEntity<ApiResponse<Product>> addProduct(@RequestBody Product product) {
        Product saved = productService.addProduct(product);
        return ResponseEntity.ok(new ApiResponse<>(true, "Product added", saved));
    }

	@GetMapping
	public ResponseEntity<ApiResponse<List<Product>>> getAllProducts(
	        org.springframework.security.core.Authentication authentication) {

	    System.out.println("Controller - Authenticated user: " + authentication);

	    List<Product> products = productService.getAllProducts();

	    return ResponseEntity.ok(
	            new ApiResponse<>(true, "Products fetched successfully", products)
	    );
	}

    
    
    @PutMapping("/{id}")
    public Product update(@PathVariable Long id, @RequestBody Product product) {
        product.setId(id);
        return productRepository.save(product);
    }
    
    @PatchMapping("/{id}/stock")
    public Product updateStock(@PathVariable Long id, @RequestParam int stock) {
        Product p = productRepository.findById(id).orElseThrow();
        p.setStock(stock);
        return productRepository.save(p);
    }
    
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        productRepository.deleteById(id);
    }
}
