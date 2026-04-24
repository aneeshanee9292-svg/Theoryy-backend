package com.zym.ecart.service;

import java.util.List;

import com.zym.ecart.entity.Product;

public interface ProductService {
	
	public Product addProduct(Product product) ;

    public List<Product> getAllProducts() ;

}
