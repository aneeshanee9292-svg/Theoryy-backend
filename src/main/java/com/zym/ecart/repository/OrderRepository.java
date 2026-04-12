package com.zym.ecart.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zym.ecart.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
	Order findByRazorpayOrderId(String razorpayOrderId);
	
}