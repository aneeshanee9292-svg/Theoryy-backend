package com.zym.ecart.service;

public interface DiscountService {

    public double applyProductDiscount(Long productId, double price);

    public double applyCouponDiscount(String couponCode, double amount);
}
