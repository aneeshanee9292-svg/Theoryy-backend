package com.zym.ecart.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponseDto {
    private Long id;
    private String mobileNumber;
    private String email;
    private String fullName;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private Double totalAmount;
    private Double discountAmount;
    private Double finalAmount;
    private Double shippingCharge;
    private String couponCode;
    private String status;
    private LocalDateTime createdAt;
    private String razorpayOrderId;
    private List<OrderItemDto> items; // ✅ clean list of items
}
