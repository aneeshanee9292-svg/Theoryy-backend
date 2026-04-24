package com.zym.ecart.controller;

import com.zym.ecart.dto.ApiResponse;
import com.zym.ecart.dto.CheckoutRequest;
import com.zym.ecart.dto.CheckoutResponse;
import com.zym.ecart.dto.OrderResponseDto;
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

 // ✅ Checkout endpoint
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<CheckoutResponse>> checkout(@Valid @RequestBody CheckoutRequest request) throws Exception {

        // Now createOrder returns OrderResponseDto
        OrderResponseDto orderDto = orderService.createOrder(request);

        CheckoutResponse response = new CheckoutResponse(
                orderDto.getId(),
                orderDto.getRazorpayOrderId(), // add this field in OrderResponseDto if needed
                orderDto.getFinalAmount()
        );

        return ResponseEntity.ok(new ApiResponse<>(true, "Order created", response));
    }

    // ✅ Get order by ID
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponseDto>> getOrder(@PathVariable Long orderId) {
        OrderResponseDto orderDto = orderService.getOrderById(orderId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Order fetched", orderDto));
    }

}