package com.zym.ecart.service.impl;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.razorpay.Utils;
import com.zym.ecart.dto.PaymentVerificationRequest;
import com.zym.ecart.entity.Order;
import com.zym.ecart.entity.Payment;
import com.zym.ecart.enums.OrderStatus;
import com.zym.ecart.repository.OrderRepository;
import com.zym.ecart.repository.PaymentRepository;
import com.zym.ecart.service.EmailService;
import com.zym.ecart.service.PaymentService;
import com.zym.ecart.service.StockService;

@Service
public class PaymentServiceImpl implements PaymentService {

		    private final OrderRepository orderRepository;
		    private final PaymentRepository paymentRepository;
		    private final StockService stockService;
		    private final EmailService emailService;

		    @Value("${razorpay.secret}")
		    private String razorpaySecret;

		    public PaymentServiceImpl(OrderRepository orderRepository, PaymentRepository paymentRepository,
					StockService stockService, EmailService emailService) {
				super();
				this.orderRepository = orderRepository;
				this.paymentRepository = paymentRepository;
				this.stockService = stockService;
				this.emailService = emailService;
			}



			@Transactional
		    @Override
		    public String verifyPayment(PaymentVerificationRequest request) throws Exception {

		        Order order = orderRepository.findByRazorpayOrderId(request.getRazorpayOrderId());

		        if (order == null) {
		            throw new RuntimeException("Order not found");
		        }

		        if (order.getStatus() == OrderStatus.PAYMENT_SUCCESS ||
		            order.getStatus() == OrderStatus.ORDER_PLACED) {
		            return "Payment already processed";
		        }
		        
		        if (order.getStatus() == OrderStatus.ORDER_PLACED) {
		            return "Order already completed";
		        }

		        String generatedSignature = Utils.getHash(
		                request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId(),
		                razorpaySecret
		        );

		        if (generatedSignature.equals(request.getRazorpaySignature())) {

		            // Update order status
		            order.setStatus(OrderStatus.PAYMENT_SUCCESS);
		            orderRepository.save(order);

		            // Save payment
		            Payment payment = Payment.builder()
		                    .orderId(order.getId())
		                    .razorpayOrderId(request.getRazorpayOrderId())
		                    .razorpayPaymentId(request.getRazorpayPaymentId())
		                    .signature(request.getRazorpaySignature())
		                    .status("SUCCESS")
		                    .amount(order.getFinalAmount())
		                    .createdAt(LocalDateTime.now())
		                    .build();

		            paymentRepository.save(payment);

		            // Reduce stock
		            stockService.reduceStock(order.getId());

		            // Final order placed
		            order.setStatus(OrderStatus.ORDER_PLACED);
		            orderRepository.save(order);
		            
		         // 🔥 SEND EMAIL
		            if (order.getEmail() != null) {
		                emailService.sendInvoice(order.getEmail(), order);
		            }

		            return "Payment verified, stock updated, order placed";
		        } else {
		            order.setStatus(OrderStatus.PAYMENT_FAILED);
		            orderRepository.save(order);
		            return "Payment verification failed";
		        }
		    }
		}