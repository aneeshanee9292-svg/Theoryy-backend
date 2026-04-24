package com.zym.ecart.service;

import com.zym.ecart.dto.CheckoutRequest;
import com.zym.ecart.dto.OrderResponseDto;

public interface OrderService {
	
	// ✅ Create order and return clean DTO
    OrderResponseDto createOrder(CheckoutRequest request) throws Exception;

    // ✅ Fetch order by ID and return DTO
    OrderResponseDto getOrderById(Long orderId);
	     
	}


