package com.zym.ecart.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.zym.ecart.entity.Order;

import java.time.LocalDateTime;

public interface OrderRepository extends JpaRepository<Order, Long> {
	Order findByRazorpayOrderId(String razorpayOrderId);

	/**
	 * Fetch orders created between two UTC timestamps in pages.
	 * Used by the daily report scheduler to stream orders in batches.
	 */
	@Query("SELECT o FROM Order o WHERE o.createdAt >= :start AND o.createdAt < :end ORDER BY o.createdAt ASC")
	Page<Order> findOrdersByCreatedAtBetween(
			@Param("start") LocalDateTime start,
			@Param("end") LocalDateTime end,
			Pageable pageable);

	/**
	 * Count orders in a date range (for report summary).
	 */
	long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime start, LocalDateTime end);
}