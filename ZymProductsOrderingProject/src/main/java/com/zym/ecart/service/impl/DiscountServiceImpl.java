package com.zym.ecart.service.impl;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.zym.ecart.entity.Coupon;
import com.zym.ecart.entity.ProductDiscount;
import com.zym.ecart.repository.CouponRepository;
import com.zym.ecart.repository.ProductDiscountRepository;
import com.zym.ecart.service.DiscountService;

@Service
public class DiscountServiceImpl implements DiscountService {

	private final ProductDiscountRepository productDiscountRepository;
	private final CouponRepository couponRepository;

	public DiscountServiceImpl(ProductDiscountRepository productDiscountRepository, CouponRepository couponRepository) {
		this.productDiscountRepository = productDiscountRepository;
		this.couponRepository = couponRepository;
	}

	@Override
	public double applyProductDiscount(Long productId, double price) {
		Optional<ProductDiscount> discountOpt = productDiscountRepository.findByProductIdAndActiveTrue(productId);

		if (discountOpt.isPresent()) {
			ProductDiscount discount = discountOpt.get();

			if ("PERCENTAGE".equals(discount.getDiscountType())) {
				return price - (price * discount.getDiscountValue() / 100);
			} else {
				return price - discount.getDiscountValue();
			}
		}else {
			System.out.println("Applying product discount for productId: " + productId);
		}
		return price;
	}

	@Override
	public double applyCouponDiscount(String couponCode, double amount) {

		if (couponCode == null || couponCode.isEmpty()) {
			return amount;
		}

		Coupon coupon = couponRepository.findByCodeAndActiveTrue(couponCode)
				.orElseThrow(() -> new RuntimeException("Invalid coupon code"));

		if (amount < coupon.getMinOrderAmount()) {
			throw new RuntimeException("Minimum order amount not met for coupon");
		}

		if ("PERCENTAGE".equals(coupon.getDiscountType())) {
			double discount = amount * coupon.getDiscountValue() / 100;
			// Apply maxDiscount cap if set
			if (coupon.getMaxDiscount() != null && coupon.getMaxDiscount() > 0) {
				discount = Math.min(discount, coupon.getMaxDiscount());
			}
			return amount - discount;
		} else {
			return amount - coupon.getDiscountValue();
		}
	}
}
