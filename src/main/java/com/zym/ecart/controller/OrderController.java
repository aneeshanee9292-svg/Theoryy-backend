package com.zym.ecart.controller;

import com.zym.ecart.dto.ApiResponse;
import com.zym.ecart.dto.CheckoutRequest;
import com.zym.ecart.dto.CheckoutResponse;
import com.zym.ecart.entity.Order;
import com.zym.ecart.repository.OrderRepository;
import com.zym.ecart.service.OrderService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@CrossOrigin
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    public OrderController(OrderService orderService, OrderRepository orderRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
    }

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<CheckoutResponse>> checkout(@Valid @RequestBody CheckoutRequest request) throws Exception {

        Order order = orderService.createOrder(request);

        CheckoutResponse response = new CheckoutResponse(
                order.getId(),
                order.getRazorpayOrderId(),
                order.getFinalAmount()
        );

        return ResponseEntity.ok(new ApiResponse<>(true, "Order created", response));
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<Order>> getOrder(@PathVariable Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        return ResponseEntity.ok(new ApiResponse<>(true, "Order fetched", order));
    }
}