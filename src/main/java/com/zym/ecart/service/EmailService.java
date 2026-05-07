package com.zym.ecart.service;

import com.zym.ecart.entity.Order;

public interface EmailService {
	public void sendInvoice(String to, Order order);
	public void sendAdminOrderNotification(Order order);
	public void sendOrderStatusUpdateEmail(String to, Order order, String oldStatus, String newStatus);
}
