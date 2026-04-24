package com.zym.ecart;

import com.zym.ecart.entity.Product;
import com.zym.ecart.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ZymProductsOrderingProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(ZymProductsOrderingProjectApplication.class, args);
	}
	/*

	@Bean
	public CommandLineRunner initData(ProductRepository productRepository) {
		return args -> {
			if (productRepository.count() == 0) {
				productRepository.save(Product.builder().name("Tiramisu Peanut Butter").price(299.0).stock(100).description("Tiramisu Peanut Butter").build());
				productRepository.save(Product.builder().name("Double Cocoa Fudge").price(299.0).stock(100).description("Double Cocoa Fudge").build());
			}
		};
	} */
} 
