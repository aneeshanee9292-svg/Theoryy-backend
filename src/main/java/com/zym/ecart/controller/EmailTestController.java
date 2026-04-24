package com.zym.ecart.controller;

import org.springframework.web.bind.annotation.*;
import com.zym.ecart.entity.Order;
import com.zym.ecart.entity.OrderItem;
import com.zym.ecart.entity.Product;
import com.zym.ecart.enums.OrderStatus;
import com.zym.ecart.service.EmailService;

import java.util.Arrays;

@RestController
@RequestMapping("/test-email")
public class EmailTestController {

    private final EmailService emailService;

    public EmailTestController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/invoice")
    public String sendTestInvoice(@RequestParam String to) {
        // 🔹 Build a dummy order object
        Order order = new Order();
        order.setId(999L);
        order.setStatus(OrderStatus.ORDER_PLACED);
        order.setFinalAmount(1234.56);

        Product product = new Product();
        product.setName("Protein Bar");

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(2);
        item.setFinalPrice(617.28);

        order.setItems(Arrays.asList(item));

        // 🔹 Call your service
        emailService.sendInvoice(to, order);

        return "Test invoice email sent to " + to;
    }
}

