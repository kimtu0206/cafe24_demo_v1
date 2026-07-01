package org.example.cafe24_demo_v1.webhook.domain.event;

import java.time.LocalDateTime;

/**
 * Cafe24 쇼핑몰에 접수된 주문의 취소 상태가 변경됐을 때 발행되는 도메인 이벤트.
 *
 * WebhookController가 Cafe24로부터 주문 취소 Webhook을 수신하면 이 이벤트를 발행한다.
 * WebhookEventService가 이 이벤트를 구독해 order 컨텍스트에 원본 payload 저장을 위임한다.
 */
public class OrderCancelledEvent {

    private final Integer eventNo;
    private final String mallId;
    private final String orderId;
    private final String payload;
    private final LocalDateTime occurredAt;

    public OrderCancelledEvent(Integer eventNo, String mallId, String orderId, String payload) {
        this.eventNo = eventNo;
        this.mallId = mallId;
        this.orderId = orderId;
        this.payload = payload;
        this.occurredAt = LocalDateTime.now();
    }

    public Integer getEventNo() { return eventNo; }
    public String getMallId() { return mallId; }
    public String getOrderId() { return orderId; }
    public String getPayload() { return payload; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
