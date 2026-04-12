package com.zym.ecart.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zym.ecart.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
	
}

