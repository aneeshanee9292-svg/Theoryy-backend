package com.zym.ecart.service.impl;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.zym.ecart.entity.Order;
import com.zym.ecart.service.EmailService;

@Service
public class EmailServiceImpl implements EmailService {

	    private final JavaMailSender mailSender;

	    public EmailServiceImpl(JavaMailSender mailSender) {
	        this.mailSender = mailSender;
	    }

	    @Override
	    @Async
	    public void sendInvoice(String to, Order order) {

	        SimpleMailMessage message = new SimpleMailMessage();
	        message.setTo(to);
	        message.setSubject("Order Invoice - #" + order.getId());

	        message.setText(
	                "Hello,\n\n" +
	                "Your order has been placed successfully.\n\n" +
	                "Order ID: " + order.getId() + "\n" +
	                "Amount: ₹" + order.getFinalAmount() + "\n" +
	                "Status: " + order.getStatus() + "\n\n" +
	                "Thank you for shopping with us!"
	        );

	        mailSender.send(message);
	    }
	}


