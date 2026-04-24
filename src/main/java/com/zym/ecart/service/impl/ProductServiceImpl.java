package com.zym.ecart.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.zym.ecart.entity.Product;
import com.zym.ecart.repository.ProductRepository;
import com.zym.ecart.service.ProductService;

@Service
public class ProductServiceImpl implements ProductService{
	
	private final ProductRepository repository;
	
	public ProductServiceImpl(ProductRepository repository) {
		super();
		this.repository = repository;
	}

	@Override
	public Product addProduct(Product product) {
		return repository.save(product);
	}

	@Override
	public List<Product> getAllProducts() {
		return repository.findAll();
	}
	
	

}
