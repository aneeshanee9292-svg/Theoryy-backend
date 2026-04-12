package com.zym.ecart.service;

import com.zym.ecart.dto.PaymentVerificationRequest;

public interface PaymentService {
	
	public String verifyPayment(PaymentVerificationRequest request) throws Exception ;
}

      
