package com.zym.ecart.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.zym.ecart.entity.Cart;
import com.zym.ecart.repository.CartRepository;
import com.zym.ecart.service.CartService;

@Service
public class CartServiceImpl implements CartService {
	
	private final CartRepository cartRepository;
	
	
	public CartServiceImpl(CartRepository cartRepository) {
		super();
		this.cartRepository = cartRepository;
	}


	@Override
	public Cart addToCart(Cart cart) {
	    return cartRepository.save(cart);
	}

	@Override
	public List<Cart> getCartBySession(String sessionId) {
	    return cartRepository.findBySessionId(sessionId);
	}

	@Override
	public void clearCart(String sessionId) {
	    List<Cart> items = cartRepository.findBySessionId(sessionId);
	    cartRepository.deleteAll(items);
	}

	// 🔥 ADD THIS METHOD (missing in your code)
	@Override
	public void removeCartItem(Long id) {
	    cartRepository.deleteById(id);
	}

}
