package com.zym.ecart.controller;

import com.zym.ecart.dto.ApiResponse;
import com.zym.ecart.dto.PaymentVerificationRequest;
import com.zym.ecart.entity.Order;
import com.zym.ecart.repository.OrderRepository;
import com.zym.ecart.service.PaymentService;
import com.zym.ecart.service.RazorpayService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    private final RazorpayService razorpayService;
    private final OrderRepository orderRepository;
    private final PaymentService paymentService;

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

        var razorpayOrder =
                razorpayService.createRazorpayOrder(order.getFinalAmount());

        order.setRazorpayOrderId(razorpayOrder.get("id"));
        orderRepository.save(order);

        return razorpayOrder.toString();
    }
    
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<String>> verifyPayment(@RequestBody PaymentVerificationRequest request) throws Exception {
        String result = paymentService.verifyPayment(request);
        return ResponseEntity.ok(new ApiResponse<>(true, result, null));
    }
    
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload,
                                                @RequestHeader("X-Razorpay-Signature") String signature) {

        System.out.println("Webhook received: " + payload);

        // Later parse JSON and update order

        return ResponseEntity.ok("OK");
    }
    
    /*@PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody PaymentVerificationRequest request) {
        try {
            String response = paymentService.verifyPayment(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    } */
}
