package org.example.cafe24_demo_v1.order.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface OrderJpaRepository extends JpaRepository<OrderEntity, Long> {

    Optional<OrderEntity> findByMallIdAndOrderId(String mallId, String orderId);
}
