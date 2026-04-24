package com.zym.ecart.controller;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zym.ecart.entity.Coupon;
import com.zym.ecart.repository.CouponRepository;

@RestController
@RequestMapping("/coupons")
@CrossOrigin
public class CouponController {

    private final CouponRepository couponRepository;

    public CouponController(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    /**
     * Public endpoint — validates a coupon code and returns discount preview.
     * Used by the frontend to show updated price before Razorpay is triggered.
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validateCoupon(
            @RequestParam String code,
            @RequestParam double amount) {

        Optional<Coupon> optCoupon = couponRepository.findByCodeAndActiveTrue(code);

        if (optCoupon.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    java.util.Map.of("valid", false, "message", "Invalid or inactive coupon code"));
        }

        Coupon coupon = optCoupon.get();

        // Check minimum order amount
        if (coupon.getMinOrderAmount() != null && amount < coupon.getMinOrderAmount()) {
            return ResponseEntity.badRequest().body(
                    java.util.Map.of("valid", false, "message",
                            "Minimum order amount is ₹" + coupon.getMinOrderAmount()));
        }

        double discount = 0;
        if ("PERCENTAGE".equals(coupon.getDiscountType())) {
            discount = amount * coupon.getDiscountValue() / 100;
            // Apply maxDiscount cap if set
            if (coupon.getMaxDiscount() != null && coupon.getMaxDiscount() > 0) {
                discount = Math.min(discount, coupon.getMaxDiscount());
            }
        } else {
            discount = coupon.getDiscountValue();
        }

        discount = Math.min(discount, amount); // can't discount more than order amount
        double finalAmount = amount - discount;

        return ResponseEntity.ok(java.util.Map.of(
                "valid", true,
                "discount", discount,
                "finalAmount", finalAmount,
                "discountType", coupon.getDiscountType(),
                "discountValue", coupon.getDiscountValue(),
                "message", "PERCENTAGE".equals(coupon.getDiscountType())
                        ? coupon.getDiscountValue() + "% off" +
                          (coupon.getMaxDiscount() != null && coupon.getMaxDiscount() > 0
                                  ? " (upto ₹" + coupon.getMaxDiscount() + ")"
                                  : "")
                        : "₹" + coupon.getDiscountValue() + " off"
        ));
    }
}
