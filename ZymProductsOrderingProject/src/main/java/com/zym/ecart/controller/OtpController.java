package com.zym.ecart.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zym.ecart.dto.ApiResponse;
import com.zym.ecart.service.impl.OtpServiceImpl;

@RestController
@RequestMapping("/otp")
@CrossOrigin
public class OtpController {

    // Inject the implementation directly (not interface) to access generateAndSendEmailOtp
    private final OtpServiceImpl otpService;

    public OtpController(OtpServiceImpl otpService) {
        this.otpService = otpService;
    }

    /**
     * Send OTP — now delivers via EMAIL (free, reliable).
     * Frontend sends: POST /otp/send?mobileNumber=9440839581&email=user@gmail.com
     *
     * The OTP is stored against the mobileNumber (for verification),
     * but DELIVERED to the user's email address.
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<String>> sendOtp(
            @RequestParam String mobileNumber,
            @RequestParam(required = false) String email) {

        if (email != null && !email.isBlank()) {
            // Send OTP via Email (FREE)
            otpService.generateAndSendEmailOtp(email, mobileNumber);
            return ResponseEntity.ok(new ApiResponse<>(true, "OTP sent to your email", null));
        } else {
            // Fallback: just generate and log to console (dev mode)
            otpService.generateOtp(mobileNumber);
            return ResponseEntity.ok(new ApiResponse<>(true, "OTP generated (check console)", null));
        }
    }

    /**
     * Verify OTP — user enters the code they received via email.
     * Verification is still by mobile number (the key used for storage).
     */
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
