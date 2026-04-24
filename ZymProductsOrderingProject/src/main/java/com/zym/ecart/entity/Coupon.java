package com.zym.ecart.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String code;

    private String discountType; // PERCENTAGE or FLAT

    private Double discountValue;

    private Double minOrderAmount;

    private Double maxDiscount; // "upto" cap for percentage coupons (e.g., 50% off up to ₹100)

    private Boolean active;
}

