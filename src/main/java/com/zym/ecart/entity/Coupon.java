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

    private Boolean active;
}

