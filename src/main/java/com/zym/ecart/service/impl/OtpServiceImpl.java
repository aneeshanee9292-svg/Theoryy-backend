package com.zym.ecart.service.impl;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.zym.ecart.service.OtpService;

import jakarta.mail.internet.MimeMessage;

@Service
public class OtpServiceImpl implements OtpService {

    // ─── In-memory stores (thread-safe) ───
    private final ConcurrentHashMap<String, String> otpStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> verifiedMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> otpExpiry = new ConcurrentHashMap<>();

    private final JavaMailSender mailSender;

    // OTP valid for 5 minutes
    private static final long OTP_VALIDITY_MS = 5 * 60 * 1000;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public OtpServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // ─── Mark a number/email as verified ───
    @Override
    public void markVerified(String identifier) {
        verifiedMap.put(identifier, true);
    }

    @Override
    public boolean isVerified(String identifier) {
        return verifiedMap.getOrDefault(identifier, false);
    }

    // ─── Generate OTP and send via Email ───
    @Override
    public String generateOtp(String mobileNumber) {
        // Generate a random 4-digit OTP
        String otp = String.valueOf(1000 + new Random().nextInt(9000));

        // Store OTP against the mobile number
        otpStore.put(mobileNumber, otp);
        otpExpiry.put(mobileNumber, System.currentTimeMillis() + OTP_VALIDITY_MS);

        // Always log to console (backup / dev use)
        System.out.println("🔑 OTP for " + mobileNumber + " is: " + otp);

        return otp;
    }

    /**
     * NEW: Generate OTP and send it to the user's EMAIL.
     * This is completely FREE — uses the Gmail SMTP already configured.
     */
    public void generateAndSendEmailOtp(String email, String mobileNumber) {
        String otp = generateOtp(mobileNumber);

        // Also store against email (so verification works with either key)
        otpStore.put(email, otp);
        otpExpiry.put(email, System.currentTimeMillis() + OTP_VALIDITY_MS);

        // Send OTP email in a background thread (non-blocking)
        new Thread(() -> {
            try {
                sendOtpEmail(email, otp);
            } catch (Exception e) {
                System.err.println("FATAL: OTP email thread crashed for " + email + ": " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    // ─── Verify OTP ───
    @Override
    public boolean verifyOtp(String mobileNumber, String otp) {
        // Check expiry
        Long expiry = otpExpiry.get(mobileNumber);
        if (expiry != null && System.currentTimeMillis() > expiry) {
            System.out.println("⏰ OTP expired for: " + mobileNumber);
            otpStore.remove(mobileNumber);
            otpExpiry.remove(mobileNumber);
            return false;
        }

        // Verify
        String storedOtp = otpStore.get(mobileNumber);
        boolean valid = otp.equals(storedOtp);

        if (valid) {
            verifiedMap.put(mobileNumber, true);
            // Clear after use (one-time OTP)
            otpStore.remove(mobileNumber);
            otpExpiry.remove(mobileNumber);
            System.out.println("✅ OTP verified for: " + mobileNumber);
        } else {
            System.out.println("❌ Invalid OTP for: " + mobileNumber
                    + " (entered: " + otp + ", expected: " + storedOtp + ")");
        }

        return valid;
    }

    // ─── Send beautiful HTML OTP email ───
    private void sendOtpEmail(String toEmail, String otp) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Your Theoryy Verification Code: " + otp);

            String html = """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="margin:0; padding:20px; background:#f5f7fa; font-family:Arial,sans-serif;">
                  <div style="max-width:480px; margin:0 auto; background:#fff; border-radius:16px;
                              overflow:hidden; box-shadow:0 4px 24px rgba(0,0,0,0.08);">

                    <!-- Header -->
                    <div style="background:linear-gradient(135deg,#FF4D4D,#B22222); padding:32px;
                                text-align:center; color:#fff;">
                      <h1 style="margin:0; font-size:24px; letter-spacing:1px;">THEORYY</h1>
                      <p style="margin:8px 0 0; font-size:14px; opacity:0.9;">Order Verification</p>
                    </div>

                    <!-- Body -->
                    <div style="padding:32px; text-align:center;">
                      <p style="color:#333; font-size:16px; margin:0 0 8px;">
                        Your verification code is:
                      </p>

                      <!-- OTP Box -->
                      <div style="background:linear-gradient(135deg,#fff0f0,#ffe5e5);
                                  border:2px dashed #ff9999; border-radius:12px;
                                  padding:20px; margin:20px 0; display:inline-block;">
                        <span style="font-size:36px; font-weight:bold; letter-spacing:12px;
                                     color:#B22222; font-family:monospace;">
                          %s
                        </span>
                      </div>

                      <p style="color:#888; font-size:13px; margin:16px 0 0;">
                        This code expires in <strong>5 minutes</strong>.<br>
                        Do not share this code with anyone.
                      </p>
                    </div>

                    <!-- Footer -->
                    <div style="background:#f9f9f9; padding:16px; text-align:center;
                                border-top:1px solid #eee; font-size:11px; color:#999;">
                      If you didn't request this code, please ignore this email.<br>
                      © Theoryy — Premium Nutrition
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(otp);

            helper.setText(html, true);
            mailSender.send(mimeMessage);

            System.out.println("📧 OTP email sent to: " + toEmail);

        } catch (Exception e) {
            System.err.println("⚠️ Failed to send OTP email to " + toEmail + ": " + e.getMessage());
        }
    }
}
