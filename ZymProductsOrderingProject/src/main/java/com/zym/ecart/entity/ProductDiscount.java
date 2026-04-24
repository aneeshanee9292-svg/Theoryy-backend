package com.zym.ecart.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_discounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDiscount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;

    private String discountType; // PERCENTAGE or FLAT

    private Double discountValue;

    private Boolean active;
}


