package org.example.cafe24_demo_v1.benefit.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 수신한 혜택 Webhook 이벤트 이력을 저장하는 JPA 엔티티.
 * eventNo + mallId + resourceId(benefitNo) 조합으로 중복 수신 여부를 판단한다.
 */
@Entity
@Table(
        name = "cafe24_benefit_webhook_event",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_no", "mall_id", "resource_id"})
)
@Getter
@Setter
class BenefitWebhookEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_no", nullable = false)
    private Integer eventNo;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "mall_id", nullable = false)
    private String mallId;

    @Column(name = "resource_id", nullable = false)
    private String resourceId = "";

    @Column(nullable = false)
    private LocalDateTime receivedAt;

    @PrePersist
    protected void onCreate() {
        receivedAt = LocalDateTime.now();
    }
}
