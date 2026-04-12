package com.zym.ecart.dto;

import lombok.Data;

@Data
public class CheckoutRequest {
    private String sessionId;
    private String mobileNumber;
    private String couponCode;
    private String email;
}
