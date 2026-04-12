package com.zym.ecart.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zym.ecart.entity.Coupon;
import com.zym.ecart.entity.ProductDiscount;
import com.zym.ecart.repository.CouponRepository;
import com.zym.ecart.repository.ProductDiscountRepository;

@RestController
@RequestMapping("/admin")
@CrossOrigin
public class AdminController {
	
	private final CouponRepository couponRepository;
	private final ProductDiscountRepository productDiscountRepository;
	
	



	public AdminController(CouponRepository couponRepository, ProductDiscountRepository productDiscountRepository) {
		super();
		this.couponRepository = couponRepository;
		this.productDiscountRepository = productDiscountRepository;
	}

	@PostMapping("/admin/coupon")
	public Coupon addCoupon(@RequestBody Coupon coupon) {
	    return couponRepository.save(coupon);
	}
	
	@PostMapping("/admin/discount")
	public ProductDiscount addDiscount(@RequestBody ProductDiscount discount) {
	    return productDiscountRepository.save(discount);
	}

}
