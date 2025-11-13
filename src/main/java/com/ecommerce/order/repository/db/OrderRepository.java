package com.ecommerce.order.repository.db;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ecommerce.order.model.db.entity.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByBuyerUserId(Long buyerUserId, Pageable pageable);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :orderId AND o.buyerUserId = :buyerUserId")
    Optional<Order> findByIdAndBuyerUserIdWithItems(Long orderId, Long buyerUserId);
}
