package com.zym.ecart.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer quantity;
    private Double price;
    private Double discount;
    private Double finalPrice;

    // 🔥 FIX: Break circular reference (child side)
    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    @JsonBackReference
    private Order order;

    // 🔥 FIX: Avoid deep recursion via Product
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
}