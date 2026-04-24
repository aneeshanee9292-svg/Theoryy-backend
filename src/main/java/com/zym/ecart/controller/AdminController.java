package com.zym.ecart.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zym.ecart.entity.Coupon;
import com.zym.ecart.entity.Order;
import com.zym.ecart.entity.ProductDiscount;
import com.zym.ecart.enums.OrderStatus;
import com.zym.ecart.dto.OrderResponseDto;
import com.zym.ecart.dto.OrderItemDto;
import com.zym.ecart.dto.ProductDto;
import com.zym.ecart.repository.CouponRepository;
import com.zym.ecart.repository.OrderRepository;
import com.zym.ecart.repository.ProductDiscountRepository;

@RestController
@RequestMapping("/admin")
public class AdminController {

	private final CouponRepository couponRepository;
	private final ProductDiscountRepository productDiscountRepository;
	private final OrderRepository orderRepository;

	public AdminController(CouponRepository couponRepository, ProductDiscountRepository productDiscountRepository,
			OrderRepository orderRepository) {
		super();
		this.couponRepository = couponRepository;
		this.productDiscountRepository = productDiscountRepository;
		this.orderRepository = orderRepository;
	}

	// ──────────────── COUPON ENDPOINTS ────────────────

	@PostMapping("/coupon")
	public Coupon addCoupon(@RequestBody Coupon coupon) {
	    return couponRepository.save(coupon);
	}

	@GetMapping("/coupon")
	public List<Coupon> getCoupons() {
	    return couponRepository.findAll();
	}

	@PutMapping("/coupon/{id}")
	public ResponseEntity<Coupon> updateCoupon(@PathVariable Long id, @RequestBody Coupon updated) {
	    return couponRepository.findById(id).map(coupon -> {
	        coupon.setCode(updated.getCode());
	        coupon.setDiscountType(updated.getDiscountType());
	        coupon.setDiscountValue(updated.getDiscountValue());
	        coupon.setMinOrderAmount(updated.getMinOrderAmount());
	        coupon.setMaxDiscount(updated.getMaxDiscount());
	        coupon.setActive(updated.getActive());
	        return ResponseEntity.ok(couponRepository.save(coupon));
	    }).orElse(ResponseEntity.notFound().build());
	}

	@DeleteMapping("/coupon/{id}")
	public ResponseEntity<Void> deleteCoupon(@PathVariable Long id) {
	    if (couponRepository.existsById(id)) {
	        couponRepository.deleteById(id);
	        return ResponseEntity.ok().build();
	    }
	    return ResponseEntity.notFound().build();
	}

	@PatchMapping("/coupon/{id}/toggle")
	public ResponseEntity<Coupon> toggleCoupon(@PathVariable Long id) {
	    return couponRepository.findById(id).map(coupon -> {
	        coupon.setActive(!coupon.getActive());
	        return ResponseEntity.ok(couponRepository.save(coupon));
	    }).orElse(ResponseEntity.notFound().build());
	}

	// ──────────────── DISCOUNT ENDPOINTS ────────────────

	@PostMapping("/discount")
	public ProductDiscount addDiscount(@RequestBody ProductDiscount discount) {
	    return productDiscountRepository.save(discount);
	}

	@GetMapping("/discount")
	public List<ProductDiscount> getDiscounts() {
	    return productDiscountRepository.findAll();
	}

	@PutMapping("/discount/{id}")
	public ResponseEntity<ProductDiscount> updateDiscount(@PathVariable Long id, @RequestBody ProductDiscount updated) {
	    return productDiscountRepository.findById(id).map(discount -> {
	        discount.setProductId(updated.getProductId());
	        discount.setDiscountType(updated.getDiscountType());
	        discount.setDiscountValue(updated.getDiscountValue());
	        discount.setActive(updated.getActive());
	        return ResponseEntity.ok(productDiscountRepository.save(discount));
	    }).orElse(ResponseEntity.notFound().build());
	}

	@DeleteMapping("/discount/{id}")
	public ResponseEntity<Void> deleteDiscount(@PathVariable Long id) {
	    if (productDiscountRepository.existsById(id)) {
	        productDiscountRepository.deleteById(id);
	        return ResponseEntity.ok().build();
	    }
	    return ResponseEntity.notFound().build();
	}

	@PatchMapping("/discount/{id}/toggle")
	public ResponseEntity<ProductDiscount> toggleDiscount(@PathVariable Long id) {
	    return productDiscountRepository.findById(id).map(discount -> {
	        discount.setActive(!discount.getActive());
	        return ResponseEntity.ok(productDiscountRepository.save(discount));
	    }).orElse(ResponseEntity.notFound().build());
	}

	// ──────────────── ORDER MANAGEMENT ENDPOINTS ────────────────

	@GetMapping("/orders")
	public List<OrderResponseDto> getAllOrders() {
	    List<Order> orders = orderRepository.findAll(
	        org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")
	    );
	    return orders.stream().map(this::convertOrderToDto).collect(Collectors.toList());
	}

	@PatchMapping("/orders/{id}/status")
	public ResponseEntity<OrderResponseDto> updateOrderStatus(
	        @PathVariable Long id, @RequestParam String status) {
	    return orderRepository.findById(id).map(order -> {
	        order.setStatus(OrderStatus.valueOf(status));
	        Order saved = orderRepository.save(order);
	        return ResponseEntity.ok(convertOrderToDto(saved));
	    }).orElse(ResponseEntity.notFound().build());
	}

	private OrderResponseDto convertOrderToDto(Order order) {
	    List<OrderItemDto> itemDtos = order.getItems().stream().map(item -> {
	        ProductDto productDto = ProductDto.builder()
	                .id(item.getProduct().getId())
	                .name(item.getProduct().getName())
	                .price(item.getProduct().getPrice())
	                .build();
	        return OrderItemDto.builder()
	                .id(item.getId())
	                .quantity(item.getQuantity())
	                .finalPrice(item.getFinalPrice())
	                .product(productDto)
	                .build();
	    }).collect(Collectors.toList());

	    return OrderResponseDto.builder()
	            .id(order.getId())
	            .mobileNumber(order.getMobileNumber())
	            .email(order.getEmail())
	            .fullName(order.getFullName())
	            .address(order.getAddress())
	            .city(order.getCity())
	            .state(order.getState())
	            .pincode(order.getPincode())
	            .totalAmount(order.getTotalAmount())
	            .discountAmount(order.getDiscountAmount())
	            .finalAmount(order.getFinalAmount())
	            .couponCode(order.getCouponCode())
	            .status(order.getStatus().name())
	            .createdAt(order.getCreatedAt())
	            .razorpayOrderId(order.getRazorpayOrderId())
	            .items(itemDtos)
	            .build();
	}
}
