package org.example.cafe24_demo_v1.webhook.domain.event;

import java.time.LocalDateTime;

/**
 * Cafe24 쇼핑몰에서 앱이 삭제됐을 때 발행되는 도메인 이벤트.
 *
 * WebhookController가 Cafe24로부터 앱 삭제 Webhook을 수신하면 이 이벤트를 발행한다.
 * WebhookEventService가 이 이벤트를 구독해 해당 인가를 REVOKED 상태로 변경한다.
 */
public class AppUninstalledEvent {

    private final String mallId;        // 앱이 삭제된 쇼핑몰 ID
    private final String clientId;      // 삭제된 앱의 클라이언트 ID
    private final LocalDateTime occurredAt; // 이벤트 발생 시각

    public AppUninstalledEvent(String mallId, String clientId) {
        this.mallId = mallId;
        this.clientId = clientId;
        this.occurredAt = LocalDateTime.now();
    }

    public String getMallId() { return mallId; }
    public String getClientId() { return clientId; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
