package com.zym.ecart.service;

public interface OtpService {
	
	public String generateOtp(String mobileNumber);
	public boolean verifyOtp(String mobileNumber, String otp);
	public void markVerified(String mobile);
	public boolean isVerified(String mobile);

}
