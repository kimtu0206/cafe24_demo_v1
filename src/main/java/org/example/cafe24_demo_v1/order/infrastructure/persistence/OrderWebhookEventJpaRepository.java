package org.example.cafe24_demo_v1.order.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface OrderWebhookEventJpaRepository extends JpaRepository<OrderWebhookEventEntity, Long> {

    boolean existsByEventNoAndMallIdAndResourceId(Integer eventNo, String mallId, String resourceId);

    List<OrderWebhookEventEntity> findByProcessedFalseOrderByIdAsc(Pageable pageable);
}
