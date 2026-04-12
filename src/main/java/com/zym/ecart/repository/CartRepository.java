package com.zym.ecart.repository;

import com.zym.ecart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CartRepository extends JpaRepository<Cart, Long> {

	List<Cart> findBySessionId(String sessionId);

}
