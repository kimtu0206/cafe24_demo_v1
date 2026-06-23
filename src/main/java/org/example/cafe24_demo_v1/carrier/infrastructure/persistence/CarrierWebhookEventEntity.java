package org.example.cafe24_demo_v1.carrier.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 수신한 배송사 Webhook 이벤트 이력을 저장하는 JPA 엔티티.
 * eventNo + mallId + resourceId(shippingCarrierCode) 조합으로 중복 수신 여부를 판단한다.
 */
@Entity
@Table(
        name = "cafe24_carrier_webhook_event",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_no", "mall_id", "resource_id"})
)
@Getter
@Setter
class CarrierWebhookEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_no", nullable = false)
    private Integer eventNo;    // Cafe24 이벤트 번호

    @Column(name = "event_type")
    private String eventType;   // 이벤트 종류 — event_no를 사람이 읽을 수 있게 보조하는 값

    @Column(name = "mall_id", nullable = false)
    private String mallId;      // 이벤트가 발생한 쇼핑몰 ID

    @Column(name = "resource_id", nullable = false)
    private String resourceId = ""; // 배송사 코드(shippingCarrierCode)

    @Column(nullable = false)
    private LocalDateTime receivedAt; // 최초 수신 시각

    @PrePersist
    protected void onCreate() {
        receivedAt = LocalDateTime.now();
    }
}
