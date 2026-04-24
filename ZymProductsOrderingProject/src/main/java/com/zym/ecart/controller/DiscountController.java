package com.zym.ecart.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zym.ecart.entity.ProductDiscount;
import com.zym.ecart.repository.ProductDiscountRepository;

@RestController
@RequestMapping("/discounts")
@CrossOrigin
public class DiscountController {

    private final ProductDiscountRepository productDiscountRepository;

    public DiscountController(ProductDiscountRepository productDiscountRepository) {
        this.productDiscountRepository = productDiscountRepository;
    }

    /**
     * Public endpoint — returns all active product discounts.
     * Used by the frontend to show discounted prices on user-facing pages.
     */
    @GetMapping("/active")
    public List<ProductDiscount> getActiveDiscounts() {
        return productDiscountRepository.findAllByActiveTrue();
    }
}
