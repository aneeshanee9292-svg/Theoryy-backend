package com.zym.ecart.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

	// ──────────────── COUPON ENDPOINTS ────────────────

	@PostMapping("/coupon")
	public Coupon addCoupon(@RequestBody Coupon coupon) {
	    return couponRepository.save(coupon);
	}

	@GetMapping("/coupon")
	public List<Coupon> getCoupons() {
	    return couponRepository.findAll();
	}

	@PutMapping("/coupon/{id}")
	public ResponseEntity<Coupon> updateCoupon(@PathVariable Long id, @RequestBody Coupon updated) {
	    return couponRepository.findById(id).map(coupon -> {
	        coupon.setCode(updated.getCode());
	        coupon.setDiscountType(updated.getDiscountType());
	        coupon.setDiscountValue(updated.getDiscountValue());
	        coupon.setMinOrderAmount(updated.getMinOrderAmount());
	        coupon.setMaxDiscount(updated.getMaxDiscount());
	        coupon.setActive(updated.getActive());
	        return ResponseEntity.ok(couponRepository.save(coupon));
	    }).orElse(ResponseEntity.notFound().build());
	}

	@DeleteMapping("/coupon/{id}")
	public ResponseEntity<Void> deleteCoupon(@PathVariable Long id) {
	    if (couponRepository.existsById(id)) {
	        couponRepository.deleteById(id);
	        return ResponseEntity.ok().build();
	    }
	    return ResponseEntity.notFound().build();
	}

	@PatchMapping("/coupon/{id}/toggle")
	public ResponseEntity<Coupon> toggleCoupon(@PathVariable Long id) {
	    return couponRepository.findById(id).map(coupon -> {
	        coupon.setActive(!coupon.getActive());
	        return ResponseEntity.ok(couponRepository.save(coupon));
	    }).orElse(ResponseEntity.notFound().build());
	}

	// ──────────────── DISCOUNT ENDPOINTS ────────────────

	@PostMapping("/discount")
	public ProductDiscount addDiscount(@RequestBody ProductDiscount discount) {
	    return productDiscountRepository.save(discount);
	}

	@GetMapping("/discount")
	public List<ProductDiscount> getDiscounts() {
	    return productDiscountRepository.findAll();
	}

	@PutMapping("/discount/{id}")
	public ResponseEntity<ProductDiscount> updateDiscount(@PathVariable Long id, @RequestBody ProductDiscount updated) {
	    return productDiscountRepository.findById(id).map(discount -> {
	        discount.setProductId(updated.getProductId());
	        discount.setDiscountType(updated.getDiscountType());
	        discount.setDiscountValue(updated.getDiscountValue());
	        discount.setActive(updated.getActive());
	        return ResponseEntity.ok(productDiscountRepository.save(discount));
	    }).orElse(ResponseEntity.notFound().build());
	}

	@DeleteMapping("/discount/{id}")
	public ResponseEntity<Void> deleteDiscount(@PathVariable Long id) {
	    if (productDiscountRepository.existsById(id)) {
	        productDiscountRepository.deleteById(id);
	        return ResponseEntity.ok().build();
	    }
	    return ResponseEntity.notFound().build();
	}

	@PatchMapping("/discount/{id}/toggle")
	public ResponseEntity<ProductDiscount> toggleDiscount(@PathVariable Long id) {
	    return productDiscountRepository.findById(id).map(discount -> {
	        discount.setActive(!discount.getActive());
	        return ResponseEntity.ok(productDiscountRepository.save(discount));
	    }).orElse(ResponseEntity.notFound().build());
	}
}
