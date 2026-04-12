package com.zym.ecart.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;

import java.time.LocalDateTime;

import com.zym.ecart.enums.OrderStatus;

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

    private Double totalAmount;

    private Double discountAmount;

    private Double finalAmount;

    private String couponCode;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private String razorpayOrderId;

    private LocalDateTime createdAt;
    
    @Email(message = "Invalid email format")
    private String email;
}
