package com.zym.ecart.service;

import com.razorpay.Order;

public interface RazorpayService {
	public Order createRazorpayOrder(double amount) throws Exception;

}
