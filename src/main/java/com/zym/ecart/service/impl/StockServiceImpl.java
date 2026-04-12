package com.zym.ecart.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zym.ecart.entity.OrderItem;
import com.zym.ecart.repository.OrderItemRepository;
import com.zym.ecart.repository.ProductRepository;
import com.zym.ecart.service.StockService;

@Service
public class StockServiceImpl implements StockService{

	    private final OrderItemRepository orderItemRepository;
	    private final ProductRepository productRepository;

	    public StockServiceImpl(OrderItemRepository orderItemRepository,
	                        ProductRepository productRepository) {
	        this.orderItemRepository = orderItemRepository;
	        this.productRepository = productRepository;
	    }

	    @Transactional
	    @Override
	    public void reduceStock(Long orderId) {

	        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);

	        for (OrderItem item : items) {
	            int updated = productRepository.reduceStock(
	                    item.getProductId(),
	                    item.getQuantity()
	            );

	            if (updated == 0) {
	                throw new RuntimeException("Not enough stock for product: " + item.getProductId());
	            }
	        }
	    }
	}


