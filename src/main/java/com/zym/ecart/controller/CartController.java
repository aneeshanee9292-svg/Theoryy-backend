package com.zym.ecart.controller;

import com.zym.ecart.dto.ApiResponse;
import com.zym.ecart.entity.Cart;
import com.zym.ecart.service.CartService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cart")
public class CartController {

	private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<Cart>> addToCart(@RequestBody Cart cart) {
        Cart saved = cartService.addToCart(cart);
        return ResponseEntity.ok(new ApiResponse<>(true, "Item added to cart", saved));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<List<Cart>>> getCart(@PathVariable String sessionId) {
        List<Cart> cart = cartService.getCartBySession(sessionId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Cart fetched", cart));
    }

    @DeleteMapping("/item/{id}")
    public ResponseEntity<ApiResponse<Void>> removeCartItem(@PathVariable Long id) {
        cartService.removeCartItem(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Item removed from cart", null));
    }

    @DeleteMapping("/clear/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> clearCart(@PathVariable String sessionId) {
        cartService.clearCart(sessionId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Cart cleared", null));
    }
}
    