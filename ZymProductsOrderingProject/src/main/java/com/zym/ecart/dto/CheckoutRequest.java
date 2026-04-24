package com.zym.ecart.dto;

import lombok.Data;

@Data
public class CheckoutRequest {
    private String sessionId;
    private String mobileNumber;
    private String couponCode;
    private String email;
    private String fullName;
    private String address;
    private String city;
    private String state;
    private String pincode;
}
