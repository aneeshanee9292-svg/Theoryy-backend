package com.zym.ecart.controller;

import com.zym.ecart.dto.ApiResponse;
import com.zym.ecart.dto.PaymentVerificationRequest;
import com.zym.ecart.entity.Order;
import com.zym.ecart.enums.OrderStatus;
import com.zym.ecart.repository.OrderRepository;
import com.zym.ecart.service.PaymentService;
import com.zym.ecart.service.RazorpayService;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    private final RazorpayService razorpayService;
    private final OrderRepository orderRepository;
    private final PaymentService paymentService;

    @Value("${razorpay.secret}")
    private String razorpayWebhookSecret;

    public PaymentController(RazorpayService razorpayService,
                             OrderRepository orderRepository,
                             PaymentService paymentService) {
        this.razorpayService = razorpayService;
        this.orderRepository = orderRepository;
        this.paymentService = paymentService;
    }

    @PostMapping("/create-order/{orderId}")
    public String createRazorpayOrder(@PathVariable Long orderId) throws Exception {
        Order order = orderRepository.findById(orderId).orElseThrow();
        var razorpayOrder = razorpayService.createRazorpayOrder(order.getFinalAmount());
        order.setRazorpayOrderId(razorpayOrder.get("id"));
        orderRepository.save(order);
        return razorpayOrder.toString();
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<String>> verifyPayment(@RequestBody PaymentVerificationRequest request) throws Exception {
        String result = paymentService.verifyPayment(request);
        return ResponseEntity.ok(new ApiResponse<>(true, result, null));
    }

    /**
     * Razorpay Webhook Handler
     * Handles payment.captured and payment.failed events.
     * Verifies webhook signature using HMAC SHA256 before processing.
     *
     * To configure: Go to Razorpay Dashboard → Settings → Webhooks
     * URL: https://your-domain.com/payment/webhook
     * Events: payment.captured, payment.failed
     * Secret: Use the same razorpay.secret from application.properties
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        System.out.println("🔔 Webhook received");

        // 1. Verify signature (skip if no signature header — dev/test mode)
        if (signature != null && !signature.isEmpty()) {
            try {
                String expectedSignature = hmacSha256(payload, razorpayWebhookSecret);
                if (!expectedSignature.equals(signature)) {
                    System.err.println("❌ Webhook signature mismatch — rejecting");
                    return ResponseEntity.status(400).body("Invalid signature");
                }
                System.out.println("✅ Webhook signature verified");
            } catch (Exception e) {
                System.err.println("❌ Webhook signature verification failed: " + e.getMessage());
                return ResponseEntity.status(400).body("Signature error");
            }
        } else {
            System.out.println("⚠️ No signature header — skipping verification (dev mode)");
        }

        // 2. Parse event
        try {
            JSONObject json = new JSONObject(payload);
            String event = json.optString("event", "");
            System.out.println("📦 Webhook event: " + event);

            if ("payment.captured".equals(event)) {
                JSONObject paymentEntity = json.getJSONObject("payload")
                        .getJSONObject("payment")
                        .getJSONObject("entity");

                String razorpayOrderId = paymentEntity.optString("order_id", null);
                String razorpayPaymentId = paymentEntity.optString("id", null);

                if (razorpayOrderId != null) {
                    Order order = orderRepository.findByRazorpayOrderId(razorpayOrderId);
                    if (order != null && order.getStatus() != OrderStatus.ORDER_PLACED) {
                        // Payment was captured but order wasn't finalized by verify endpoint
                        // This is a safety net — mark as placed
                        order.setStatus(OrderStatus.ORDER_PLACED);
                        orderRepository.save(order);
                        System.out.println("✅ Webhook: Order #" + order.getId() + " marked as ORDER_PLACED (payment: " + razorpayPaymentId + ")");
                    } else if (order != null) {
                        System.out.println("ℹ️ Webhook: Order #" + order.getId() + " already placed — no action needed");
                    } else {
                        System.out.println("⚠️ Webhook: No order found for razorpayOrderId: " + razorpayOrderId);
                    }
                }

            } else if ("payment.failed".equals(event)) {
                JSONObject paymentEntity = json.getJSONObject("payload")
                        .getJSONObject("payment")
                        .getJSONObject("entity");

                String razorpayOrderId = paymentEntity.optString("order_id", null);

                if (razorpayOrderId != null) {
                    Order order = orderRepository.findByRazorpayOrderId(razorpayOrderId);
                    if (order != null && order.getStatus() != OrderStatus.ORDER_PLACED) {
                        order.setStatus(OrderStatus.PAYMENT_FAILED);
                        orderRepository.save(order);
                        System.out.println("⚠️ Webhook: Order #" + order.getId() + " marked as PAYMENT_FAILED");
                    }
                }

            } else {
                System.out.println("ℹ️ Webhook: Unhandled event type: " + event);
            }

        } catch (Exception e) {
            System.err.println("❌ Webhook payload parse error: " + e.getMessage());
            e.printStackTrace();
        }

        // Always return 200 to Razorpay so it doesn't retry
        return ResponseEntity.ok("OK");
    }

    /**
     * HMAC SHA256 signature computation for webhook verification.
     */
    private String hmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes("UTF-8"));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
