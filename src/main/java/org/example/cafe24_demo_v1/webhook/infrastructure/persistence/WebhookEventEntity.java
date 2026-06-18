package org.example.cafe24_demo_v1.webhook.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 수신한 Webhook 이벤트 이력을 저장하는 JPA 엔티티.
 * eventNo + mallId + resourceId 조합으로 중복 수신 여부를 판단한다.
 * resourceId는 같은 eventNo가 리소스마다 반복 발생하는 경우(예: 상품 생성)
 * 리소스를 구분하기 위한 값이며, 해당하는 리소스가 없는 이벤트(예: 앱 삭제)는 빈 문자열을 사용한다.
 */
@Entity
@Table(
        name = "cafe24_webhook_event",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_no", "mall_id", "resource_id"})
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

    @Column(name = "resource_id", nullable = false)
    private String resourceId = ""; // 이벤트 대상 리소스 식별자 (예: product_no). 없으면 빈 문자열

    @Column(nullable = false)
    private LocalDateTime receivedAt; // 최초 수신 시각

    @PrePersist
    protected void onCreate() {
        receivedAt = LocalDateTime.now();
    }
}
