package com.zym.ecart.service;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.zym.ecart.dto.ApiResponse;
import com.zym.ecart.entity.Cart;


public interface CartService {

	public Cart addToCart(Cart cart) ;

	public List<Cart> getCartBySession(String sessionId);

	public void clearCart(String sessionId);
    
    public void removeCartItem(Long id);
}
