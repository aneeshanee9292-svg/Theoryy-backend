package com.zym.ecart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ZymProductsOrderingProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(ZymProductsOrderingProjectApplication.class, args);
	}

}
