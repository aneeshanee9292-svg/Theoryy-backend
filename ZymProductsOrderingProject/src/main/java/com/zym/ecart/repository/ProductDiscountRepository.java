package com.zym.ecart.repository;

import com.zym.ecart.entity.ProductDiscount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductDiscountRepository extends JpaRepository<ProductDiscount, Long> {

    Optional<ProductDiscount> findByProductIdAndActiveTrue(Long productId);

    java.util.List<ProductDiscount> findAllByActiveTrue();
}