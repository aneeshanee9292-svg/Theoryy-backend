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

    public EmailServiceImpl(JavaMailSender mailSender,
                            OrderItemRepository orderItemRepository) {
        this.mailSender = mailSender;
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    @Async
    public void sendInvoice(String to, Order order) {
        try {
            System.out.println("📧 Sending email to: " + to);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Order Invoice - #" + order.getId());

            StringBuilder html = new StringBuilder();

            html.append("<!DOCTYPE html>");
            html.append("<html><head><meta charset='UTF-8'>");
            html.append("<style>");
            html.append("body { font-family: Arial, sans-serif; background-color:#f5f7fa; margin:0; padding:20px; }");
            html.append(".container { max-width:600px; margin:0 auto; background:#fff; border-radius:10px; overflow:hidden; box-shadow:0 4px 10px rgba(0,0,0,0.1);} ");
            html.append(".header { background:linear-gradient(135deg, #FF4D4D, #B22222); color:#fff; text-align:center; padding:30px;} ");
            html.append(".header h2 { margin:0; font-size:24px;} ");
            html.append(".header p { margin:8px 0 0; font-size:14px;} ");
            /* Progress tracker */
            html.append(".progress-container { display:flex; justify-content:space-between; align-items:center; padding:30px 40px; position:relative; }");
            html.append(".progress-container::before { content:''; position:absolute; top:50%; left:40px; right:40px; height:2px; background:#ddd; z-index:0; }");
            html.append(".progress-step { width:33%; text-align:center; position:relative; z-index:1; }");
            html.append(".progress-icon { width:40px; height:40px; line-height:40px; border-radius:50%; background:#B22222; color:#fff; display:inline-block; font-size:18px; font-weight:bold; }");
            html.append(".progress-label { display:block; margin-top:8px; font-size:13px; font-weight:bold; color:#B22222;} ");
            html.append(".card { background:#fff; border:1px solid #eee; border-radius:8px; padding:15px; margin:15px; box-shadow:0 1px 4px rgba(0,0,0,0.05);} ");
            html.append(".card h3 { margin:0 0 10px 0; font-size:16px; color:#B22222;} ");
            html.append(".amount-card { background:linear-gradient(135deg, #fff0f0, #ffe5e5); border:1px solid #ff9999; text-align:center; padding:20px; font-size:20px; font-weight:bold; color:#B22222; border-radius:10px; box-shadow:0 2px 6px rgba(0,0,0,0.1); margin:15px;} ");
            html.append("table { width:100%; border-collapse:collapse; margin-top:15px;} ");
            html.append("th, td { padding:12px; border-bottom:1px solid #eee; font-size:14px;} ");
            html.append("th { background:#f9f9f9; text-align:left; color:#B22222;} ");
            html.append(".btn { display:inline-block; margin:25px auto; padding:12px 25px; background:#B22222; color:#fff; text-decoration:none; border-radius:6px; font-weight:bold;} ");
            html.append(".footer { background:#f9f9f9; text-align:center; padding:20px; font-size:13px; color:#555; border-top:2px solid #B22222;} ");
            html.append("</style></head><body>");

            html.append("<div class='container'>");

            // ✅ Logo
            html.append("<div style='text-align:center; margin:20px;'>")
                .append("<img src='https://raw.githubusercontent.com/udayKumar1302/Theory/main/theoryy-logo.png' alt='Company Logo' style='height:60px;'>")
                .append("</div>");

            // ✅ Header
            html.append("<div class='header'>");
            html.append("<h2>Thanks for Ordering!</h2>");
            html.append("<p>Hi Customer, we’ve received your order and are getting it ready to ship.</p>");
            html.append("</div>");

            // ✅ Status Progress Tracker (start, center, end)
            html.append("<div class='progress-container'>");
            html.append("<div class='progress-step'><span class='progress-icon'>✔</span><span class='progress-label'>Order Confirmed</span></div>");
            html.append("<div class='progress-step'><span class='progress-icon'>🚚</span><span class='progress-label'>Shipping</span></div>");
            html.append("<div class='progress-step'><span class='progress-icon'>📦</span><span class='progress-label'>Arrived</span></div>");
            html.append("</div>");

            // ✅ Order ID Card
            html.append("<div class='card'>");
            html.append("<h3>Order ID</h3>");
            html.append("<p>#").append(order.getId()).append("</p>");
            html.append("</div>");

            // ✅ Status Card
            html.append("<div class='card'>");
            html.append("<h3>Status</h3>");
            html.append("<p>").append(order.getStatus()).append("</p>");
            html.append("</div>");

            // ✅ Amount Card
            html.append("<div class='amount-card'>");
            html.append("Total Amount: ₹").append(order.getFinalAmount());
            html.append("</div>");

            // ✅ Products Table
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            html.append("<div class='card'>");
            html.append("<h3>Your Order Details</h3>");
            html.append("<table>");
            html.append("<tr><th>Product</th><th>Quantity</th><th>Price</th></tr>");
            for (OrderItem item : items) {
                String productName = (item.getProduct() != null)
                        ? item.getProduct().getName()
                        : "Product";
                html.append("<tr>")
                    .append("<td>").append(productName).append("</td>")
                    .append("<td>").append(item.getQuantity()).append("</td>")
                    .append("<td>₹").append(item.getFinalPrice()).append("</td>")
                    .append("</tr>");
            }
            html.append("</table>");
            html.append("</div>");

            // ✅ Track Order Button
            html.append("<div style='text-align:center;'>");
            html.append("<a href='#' class='btn'>Track Order</a>");
            html.append("</div>");

            // ✅ Footer
            html.append("<div class='footer'>");
            html.append("We appreciate your trust in us.<br>");
            html.append("We hope to see you again soon — your next order awaits!<br>");
            html.append("— The E‑Cart Team");
            html.append("</div>");

            html.append("</div></body></html>");

            helper.setText(html.toString(), true);

            mailSender.send(mimeMessage);

            System.out.println("✅ Email sent successfully for order: " + order.getId());

        } catch (Exception e) {
            System.err.println("❌ Email sending failed!");
            e.printStackTrace();
        }
    }
}
