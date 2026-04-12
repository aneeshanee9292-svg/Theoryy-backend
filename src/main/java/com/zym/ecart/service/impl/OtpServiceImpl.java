package com.zym.ecart.service.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.zym.ecart.service.OtpService;

@Service
public class OtpServiceImpl implements OtpService{
	
	private final Map<String, String> otpStore = new HashMap<>();
	private final Map<String, Boolean> verifiedMap = new HashMap<>();
	
	@Override
	public void markVerified(String mobile) {
	    verifiedMap.put(mobile, true);
	}

	@Override
	public boolean isVerified(String mobile) {
	    return verifiedMap.getOrDefault(mobile, false);
	}

	@Override
    public String generateOtp(String mobileNumber) {
        String otp = String.valueOf(1000 + new Random().nextInt(9000));
        otpStore.put(mobileNumber, otp);

        // 🔥 For now print OTP (simulate SMS)
        System.out.println("OTP for " + mobileNumber + " is: " + otp);

        return otp;
    }

	@Override
    public boolean verifyOtp(String mobileNumber, String otp) {
		 boolean valid = otp.equals(otpStore.get(mobileNumber));

		    if (valid) {
		        verifiedMap.put(mobileNumber, true);
		    }

		    return valid;
    }

}
