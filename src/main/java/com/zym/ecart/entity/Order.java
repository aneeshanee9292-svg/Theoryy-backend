package com.zym.ecart.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.zym.ecart.enums.OrderStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String mobileNumber;

    private String fullName;
    private String address;
    private String city;
    private String state;
    private String pincode;

    private Double totalAmount;

    private Double discountAmount;

    private Double finalAmount;

    private Double shippingCharge;

    // 🔥 FIX: Prevent infinite recursion (parent side)
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    private String couponCode;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private String razorpayOrderId;

    private LocalDateTime createdAt;

    @Email(message = "Invalid email format")
    private String email;
}