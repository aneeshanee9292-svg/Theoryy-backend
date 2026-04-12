package com.zym.ecart.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    private String razorpayPaymentId;

    private String razorpayOrderId;

    private String signature;

    private String status;

    private Double amount;

    private LocalDateTime createdAt;
}
