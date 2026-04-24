package com.zym.ecart.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.zym.ecart.dto.CheckoutRequest;
import com.zym.ecart.dto.OrderItemDto;
import com.zym.ecart.dto.OrderResponseDto;
import com.zym.ecart.dto.ProductDto;
import com.zym.ecart.entity.Cart;
import com.zym.ecart.entity.Order;
import com.zym.ecart.entity.OrderItem;
import com.zym.ecart.entity.Product;
import com.zym.ecart.enums.OrderStatus;
import com.zym.ecart.repository.CartRepository;
import com.zym.ecart.repository.OrderItemRepository;
import com.zym.ecart.repository.OrderRepository;
import com.zym.ecart.repository.ProductRepository;
import com.zym.ecart.service.DiscountService;
import com.zym.ecart.service.OrderService;
import com.zym.ecart.service.OtpService;
import com.zym.ecart.service.RazorpayService;

import jakarta.transaction.Transactional;

@Service
public class OrderServiceImpl implements OrderService {

	private final CartRepository cartRepository;
	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final DiscountService discountService;
	private final RazorpayService razorpayService;
	private final OtpService otpService;
	private final ProductRepository productRepository;
	private static final Logger log = LoggerFactory.getLogger(OrderService.class);

	public OrderServiceImpl(CartRepository cartRepository, OrderRepository orderRepository,
			OrderItemRepository orderItemRepository, DiscountService discountService, RazorpayService razorpayService,
			OtpService otpService, ProductRepository productRepository) {
		super();
		this.cartRepository = cartRepository;
		this.orderRepository = orderRepository;
		this.orderItemRepository = orderItemRepository;
		this.discountService = discountService;
		this.razorpayService = razorpayService;
		this.otpService = otpService;
		this.productRepository = productRepository;
	}

	@Transactional
	public OrderResponseDto createOrder(CheckoutRequest request) throws Exception {

		// ✅ Step 1: Verify OTP before anything else
		if (!otpService.isVerified(request.getMobileNumber())) {
			throw new RuntimeException("OTP not verified");
		}

		// Step 2: Fetch cart items
		List<Cart> cartItems = cartRepository.findBySessionId(request.getSessionId());
		if (cartItems == null || cartItems.isEmpty()) {
			throw new RuntimeException("No cart items found for sessionId: " + request.getSessionId());
		}

		double totalAmount = 0;
		double discountAmount = 0;

		// Step 3: Calculate totals and discounts
		for (Cart cart : cartItems) {
			Product product = productRepository.findById(cart.getProductId())
					.orElseThrow(() -> new RuntimeException("Product not found"));

			double price = product.getPrice() * cart.getQuantity();

			double discountedPrice = discountService.applyProductDiscount(cart.getProductId(), price);
			totalAmount += price;
			discountAmount += (price - discountedPrice);
		}

		double finalAmount = discountService.applyCouponDiscount(request.getCouponCode(), totalAmount - discountAmount);

		// Step 4: Create and save order
		Order order = Order.builder()
				.mobileNumber(request.getMobileNumber())
				.email(request.getEmail())
				.fullName(request.getFullName())
				.address(request.getAddress())
				.city(request.getCity())
				.state(request.getState())
				.pincode(request.getPincode())
				.totalAmount(totalAmount)
				.discountAmount(discountAmount)
				.finalAmount(finalAmount)
				.couponCode(request.getCouponCode())
				.status(OrderStatus.PAYMENT_PENDING)
				.createdAt(LocalDateTime.now())
				.build();

		Order savedOrder = orderRepository.save(order);

		// Step 5: Razorpay integration
		com.razorpay.Order razorpayOrder = razorpayService
				.createRazorpayOrder((long) (savedOrder.getFinalAmount() * 100));
		savedOrder.setRazorpayOrderId(razorpayOrder.get("id"));
		orderRepository.save(savedOrder);

		// Step 6: Save order items
		for (Cart cart : cartItems) {
			Product product = productRepository.findById(cart.getProductId())
					.orElseThrow(() -> new RuntimeException("Product not found"));

			double price = product.getPrice() * cart.getQuantity();

			double discountedPrice = discountService.applyProductDiscount(cart.getProductId(), price);

			OrderItem item = OrderItem.builder().order(savedOrder) // ✅ set relation
					.product(product) // ✅ set relation
					.quantity(cart.getQuantity()).price(price).discount(price - discountedPrice)
					.finalPrice(discountedPrice).build();

			orderItemRepository.save(item);
		}

		log.info("Creating order for session: {}", request.getSessionId());

		// Step 7: Clear cart
		cartRepository.deleteAll(cartItems);

		return convertOrderToDto(savedOrder);
	}

	@Override
	public OrderResponseDto getOrderById(Long orderId) {
	    Order order = orderRepository.findById(orderId)
	            .orElseThrow(() -> new RuntimeException("Order not found"));
	    return convertOrderToDto(order); // ✅ use helper method
	}

	// Convert Product → ProductDto
	private ProductDto convertProductToDto(Product product) {
		return ProductDto.builder().id(product.getId()).name(product.getName()).price(product.getPrice()).build();
	}

	// Convert OrderItem → OrderItemDto
	private OrderItemDto convertOrderItemToDto(OrderItem item) {
		return OrderItemDto.builder().id(item.getId()).quantity(item.getQuantity()).finalPrice(item.getFinalPrice())
				.product(convertProductToDto(item.getProduct())).build();
	}

	private OrderResponseDto convertOrderToDto(Order order) {
		List<OrderItemDto> itemDtos = order.getItems().stream().map(this::convertOrderItemToDto)
				.collect(java.util.stream.Collectors.toList());

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
