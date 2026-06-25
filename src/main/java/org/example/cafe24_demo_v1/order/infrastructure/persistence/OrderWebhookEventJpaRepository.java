package org.example.cafe24_demo_v1.order.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

interface OrderWebhookEventJpaRepository extends JpaRepository<OrderWebhookEventEntity, Long> {

    boolean existsByEventNoAndMallIdAndResourceId(Integer eventNo, String mallId, String resourceId);

    @Query("""
            SELECT e FROM OrderWebhookEventEntity e
            WHERE (e.status IN ('RECEIVED', 'FAILED') AND (e.nextRetryAt IS NULL OR e.nextRetryAt <= :now))
            OR (e.status = 'PROCESSING' AND e.lastTriedAt <= :processingStaleBefore)
            ORDER BY e.id ASC
            """)
    List<OrderWebhookEventEntity> findRetryable(
            @Param("now") LocalDateTime now,
            @Param("processingStaleBefore") LocalDateTime processingStaleBefore,
            Pageable pageable
    );

    long countByStatus(String status);

    @Query("SELECT COALESCE(SUM(e.retryCount), 0) FROM OrderWebhookEventEntity e")
    long sumRetryCount();
}
