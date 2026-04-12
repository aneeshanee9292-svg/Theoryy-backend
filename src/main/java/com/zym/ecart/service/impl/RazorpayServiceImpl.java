package com.zym.ecart.service.impl;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.zym.ecart.service.RazorpayService;

import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class RazorpayServiceImpl implements RazorpayService {

    private final RazorpayClient razorpayClient;

    public RazorpayServiceImpl(RazorpayClient razorpayClient) {
        this.razorpayClient = razorpayClient;
    }

    public Order createRazorpayOrder(double amount) throws Exception {

        JSONObject options = new JSONObject();
        options.put("amount", (int)(amount * 100)); // in paise
        options.put("currency", "INR");
        options.put("receipt", "order_rcptid_" + System.currentTimeMillis());

        return razorpayClient.orders.create(options);
    }
}