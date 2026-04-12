package com.zym.ecart.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zym.ecart.dto.ApiResponse;
import com.zym.ecart.service.OtpService;

@RestController
@RequestMapping("/otp")
@CrossOrigin
public class OtpController {

    private final OtpService otpService;

    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<String>> sendOtp(@RequestParam String mobileNumber) {
        otpService.generateOtp(mobileNumber);
        return ResponseEntity.ok(new ApiResponse<>(true, "OTP sent", null));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Boolean>> verifyOtp(
            @RequestParam String mobileNumber,
            @RequestParam String otp) {

        boolean isValid = otpService.verifyOtp(mobileNumber, otp);

        if (!isValid) {
            throw new RuntimeException("Invalid OTP");
        }

        return ResponseEntity.ok(new ApiResponse<>(true, "OTP verified", true));
    }
}
