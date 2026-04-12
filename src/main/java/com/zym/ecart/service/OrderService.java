package com.zym.ecart.service;

import com.zym.ecart.dto.CheckoutRequest;
import com.zym.ecart.entity.Order;

public interface OrderService {
	
	public Order createOrder(CheckoutRequest request) throws Exception;
	     
	}


