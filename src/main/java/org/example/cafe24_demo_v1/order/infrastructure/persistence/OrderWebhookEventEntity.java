package org.example.cafe24_demo_v1.order.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DB 테이블(cafe24_order_webhook_event)과 매핑되는 JPA 엔티티.
 * Webhook으로 수신한 주문 원본 데이터와 처리 상태(재처리용)를 보관한다.
 */
@Entity
@Table(
        name = "cafe24_order_webhook_event",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_no", "mall_id", "resource_id"})
)
@Getter
@Setter
class OrderWebhookEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mall_id", nullable = false)
    private String mallId;

    @Column(name = "event_no", nullable = false)
    private Integer eventNo;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "resource_id", nullable = false)
    private String resourceId;   // Cafe24 주문번호(order_id)

    @Column(name = "webhook_id")
    private String webhookId;    // Cafe24가 제공하면 채워짐(현재 payload에는 없음)

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "status", nullable = false)
    private String status;    // OrderWebhookEventStatus.name() — RECEIVED/PROCESSING/PROCESSED/FAILED/DEAD

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "last_tried_at")
    private LocalDateTime lastTriedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
