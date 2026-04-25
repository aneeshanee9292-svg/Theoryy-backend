package com.zym.ecart.service.impl;

import com.zym.ecart.entity.Order;
import com.zym.ecart.entity.OrderItem;
import com.zym.ecart.repository.OrderItemRepository;
import com.zym.ecart.service.EmailService;

import jakarta.mail.internet.MimeMessage;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final OrderItemRepository orderItemRepository;

    private static final String ADMIN_EMAIL = "udaykumararipaka@gmail.com";

    public EmailServiceImpl(JavaMailSender mailSender,
                            OrderItemRepository orderItemRepository) {
        this.mailSender = mailSender;
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    @Async
    public void sendInvoice(String to, Order order) {
        try {
            System.out.println("📧 Sending invoice email to: " + to);
            String html = buildInvoiceHtml(order, false);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("Order Confirmed - #" + order.getId() + " | THEORYY");
            helper.setText(html, true);
            mailSender.send(mimeMessage);

            System.out.println("✅ Invoice email sent for order: " + order.getId());
        } catch (Exception e) {
            System.err.println("❌ Invoice email failed!");
            e.printStackTrace();
        }
    }

    @Override
    @Async
    public void sendAdminOrderNotification(Order order) {
        try {
            System.out.println("📧 Sending admin notification for order: " + order.getId());
            String html = buildInvoiceHtml(order, true);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(ADMIN_EMAIL);
            helper.setSubject("🛒 New Order #" + order.getId() + " — ₹" + order.getFinalAmount() + " | THEORYY Admin");
            helper.setText(html, true);
            mailSender.send(mimeMessage);

            System.out.println("✅ Admin notification sent for order: " + order.getId());
        } catch (Exception e) {
            System.err.println("❌ Admin notification email failed!");
            e.printStackTrace();
        }
    }

    private String buildInvoiceHtml(Order order, boolean isAdmin) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<style>");
        html.append("body { font-family: 'Segoe UI', Arial, sans-serif; background-color:#f8f9fa; margin:0; padding:20px; color:#333; }");
        html.append(".container { max-width:600px; margin:0 auto; background:#ffffff; border-radius:16px; overflow:hidden; box-shadow:0 8px 30px rgba(0,0,0,0.08); }");
        html.append(".header { background:linear-gradient(135deg, #1a1a1a, #2d2d2d); color:#fff; text-align:center; padding:35px 30px; }");
        html.append(".header h1 { margin:0; font-size:28px; font-weight:700; letter-spacing:3px; }");
        html.append(".header p { margin:10px 0 0; font-size:14px; opacity:0.85; }");
        html.append(".status-banner { text-align:center; padding:20px; background:linear-gradient(135deg, #e8f5e9, #c8e6c9); border-bottom:1px solid #a5d6a7; }");
        html.append(".status-banner.admin { background:linear-gradient(135deg, #fff3e0, #ffe0b2); border-bottom:1px solid #ffcc80; }");
        html.append(".status-badge { display:inline-block; padding:8px 24px; border-radius:50px; font-size:14px; font-weight:700; letter-spacing:1px; }");
        html.append(".status-badge.success { background:#4caf50; color:#fff; }");
        html.append(".status-badge.admin { background:#ff9800; color:#fff; }");
        html.append(".section { padding:25px 30px; }");
        html.append(".section-title { font-size:11px; text-transform:uppercase; letter-spacing:2px; color:#999; font-weight:700; margin-bottom:12px; }");
        html.append(".info-grid { display:table; width:100%; }");
        html.append(".info-row { display:table-row; }");
        html.append(".info-label { display:table-cell; padding:6px 0; font-size:13px; color:#888; width:120px; }");
        html.append(".info-value { display:table-cell; padding:6px 0; font-size:13px; font-weight:600; color:#333; }");
        html.append(".divider { height:1px; background:#eee; margin:0; }");
        html.append("table { width:100%; border-collapse:collapse; }");
        html.append("th { padding:12px 8px; font-size:11px; text-transform:uppercase; letter-spacing:1px; color:#888; font-weight:700; text-align:left; border-bottom:2px solid #eee; }");
        html.append("td { padding:14px 8px; font-size:14px; border-bottom:1px solid #f5f5f5; }");
        html.append(".footer { background:#1a1a1a; color:#aaa; text-align:center; padding:25px; font-size:12px; }");
        html.append(".footer a { color:#fff; text-decoration:none; }");
        html.append("</style></head><body>");

        html.append("<div class='container'>");

        // Logo
        html.append("<div style='text-align:center; padding:20px 0 0;'>")
            .append("<img src='https://raw.githubusercontent.com/udayKumar1302/Theory/main/theoryy-logo.png' alt='THEORYY' style='height:50px;'>")
            .append("</div>");

        // Header
        html.append("<div class='header'>");
        if (isAdmin) {
            html.append("<h1>NEW ORDER RECEIVED</h1>");
            html.append("<p>A customer just placed an order on your store</p>");
        } else {
            html.append("<h1>THANK YOU!</h1>");
            html.append("<p>Your order has been confirmed and is being processed</p>");
        }
        html.append("</div>");

        // Status Banner
        html.append("<div class='status-banner").append(isAdmin ? " admin" : "").append("'>");
        html.append("<span class='status-badge ").append(isAdmin ? "admin" : "success").append("'>");
        html.append(isAdmin ? "⚡ ORDER #" + order.getId() : "✓ ORDER CONFIRMED");
        html.append("</span>");
        html.append("</div>");

        // Order Info
        html.append("<div class='section'>");
        html.append("<div class='section-title'>Order Details</div>");
        html.append("<div class='info-grid'>");
        addInfoRow(html, "Order ID", "#" + order.getId());
        addInfoRow(html, "Status", String.valueOf(order.getStatus()));
        if (order.getRazorpayOrderId() != null) {
            addInfoRow(html, "Payment ID", order.getRazorpayOrderId());
        }
        addInfoRow(html, "Date", order.getCreatedAt() != null ? order.getCreatedAt().toString() : "N/A");
        html.append("</div>");
        html.append("</div>");

        html.append("<div class='divider'></div>");

        // Customer Info
        html.append("<div class='section'>");
        html.append("<div class='section-title'>").append(isAdmin ? "Customer Information" : "Delivery To").append("</div>");
        html.append("<div class='info-grid'>");
        addInfoRow(html, "Name", order.getFullName() != null ? order.getFullName() : "N/A");
        addInfoRow(html, "Email", order.getEmail() != null ? order.getEmail() : "N/A");
        addInfoRow(html, "Phone", order.getMobileNumber() != null ? order.getMobileNumber() : "N/A");
        String address = String.join(", ",
            order.getAddress() != null ? order.getAddress() : "",
            order.getCity() != null ? order.getCity() : "",
            order.getState() != null ? order.getState() : "",
            order.getPincode() != null ? order.getPincode() : ""
        );
        addInfoRow(html, "Address", address);
        html.append("</div>");
        html.append("</div>");

        html.append("<div class='divider'></div>");

        // Items Table
        html.append("<div class='section'>");
        html.append("<div class='section-title'>Items Ordered</div>");
        html.append("<table>");
        html.append("<tr><th>Product</th><th style='text-align:center;'>Qty</th><th style='text-align:right;'>Price</th></tr>");
        for (OrderItem item : items) {
            String productName = (item.getProduct() != null) ? item.getProduct().getName() : "Product";
            html.append("<tr>")
                .append("<td>").append(productName).append("</td>")
                .append("<td style='text-align:center;'>").append(item.getQuantity()).append("</td>")
                .append("<td style='text-align:right; font-weight:600;'>₹").append(item.getFinalPrice()).append("</td>")
                .append("</tr>");
        }
        html.append("</table>");
        html.append("</div>");

        // Amount Section
        html.append("<div style='background:linear-gradient(135deg, #fafafa, #f5f5f5); padding:25px 30px;'>");
        if (order.getTotalAmount() != null) {
            html.append("<div style='display:flex; justify-content:space-between; padding:6px 0; font-size:14px;'>");
            html.append("<span style='color:#888;'>Subtotal</span>");
            html.append("<span>₹").append(order.getTotalAmount()).append("</span>");
            html.append("</div>");
        }
        if (order.getDiscountAmount() != null && order.getDiscountAmount() > 0) {
            html.append("<div style='display:flex; justify-content:space-between; padding:6px 0; font-size:14px; color:#4caf50;'>");
            html.append("<span>Discount");
            if (order.getCouponCode() != null) {
                html.append(" (").append(order.getCouponCode()).append(")");
            }
            html.append("</span>");
            html.append("<span>-₹").append(order.getDiscountAmount()).append("</span>");
            html.append("</div>");
        }
        html.append("<div style='display:flex; justify-content:space-between; padding:12px 0; font-size:20px; font-weight:700; color:#1a1a1a; border-top:2px solid #ddd; margin-top:8px;'>");
        html.append("<span>Total</span>");
        html.append("<span>₹").append(order.getFinalAmount()).append("</span>");
        html.append("</div>");
        html.append("</div>");

        // Footer
        html.append("<div class='footer'>");
        if (isAdmin) {
            html.append("This is an automated admin notification.<br>");
            html.append("Log in to your <a href='#'>Admin Dashboard</a> to manage this order.");
        } else {
            html.append("We appreciate your trust in us!<br>");
            html.append("We hope to see you again soon — your next order awaits!<br><br>");
            html.append("<strong>— Team THEORYY</strong>");
        }
        html.append("</div>");

        html.append("</div></body></html>");

        return html.toString();
    }

    private void addInfoRow(StringBuilder html, String label, String value) {
        html.append("<div class='info-row'>")
            .append("<div class='info-label'>").append(label).append("</div>")
            .append("<div class='info-value'>").append(value != null ? value : "—").append("</div>")
            .append("</div>");
    }
}
