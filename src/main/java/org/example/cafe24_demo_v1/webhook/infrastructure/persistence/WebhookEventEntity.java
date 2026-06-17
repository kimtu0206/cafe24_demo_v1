package org.example.cafe24_demo_v1.webhook.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 수신한 Webhook 이벤트 이력을 저장하는 JPA 엔티티.
 * eventNo + mallId 조합으로 중복 수신 여부를 판단한다.
 */
@Entity
@Table(
        name = "cafe24_webhook_event",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_no", "mall_id"})
)
@Getter
@Setter
class WebhookEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_no", nullable = false)
    private Integer eventNo;    // Cafe24 이벤트 번호

    @Column(name = "mall_id", nullable = false)
    private String mallId;      // 이벤트가 발생한 쇼핑몰 ID

    @Column(nullable = false)
    private LocalDateTime receivedAt; // 최초 수신 시각

    @PrePersist
    protected void onCreate() {
        receivedAt = LocalDateTime.now();
    }
}
