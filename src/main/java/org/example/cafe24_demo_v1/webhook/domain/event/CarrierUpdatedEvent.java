package org.example.cafe24_demo_v1.webhook.domain.event;

import java.time.LocalDateTime;

/**
 * Cafe24 쇼핑몰의 배송사 정보가 수정됐을 때 발행되는 도메인 이벤트.
 *
 * WebhookController가 Cafe24로부터 배송사 수정 Webhook을 수신하면 이 이벤트를 발행한다.
 * WebhookEventService가 이 이벤트를 구독해 CarrierService로 상세 정보를 다시 조회/저장한다.
 */
public class CarrierUpdatedEvent {

    private final Integer eventNo;            // Cafe24 이벤트 번호 (중복 수신 판단에 사용)
    private final String mallId;               // 배송사가 등록된 쇼핑몰 ID
    private final String shippingCarrierCode;   // 수정된 배송사 코드
    private final LocalDateTime occurredAt;

    public CarrierUpdatedEvent(Integer eventNo, String mallId, String shippingCarrierCode) {
        this.eventNo = eventNo;
        this.mallId = mallId;
        this.shippingCarrierCode = shippingCarrierCode;
        this.occurredAt = LocalDateTime.now();
    }

    public Integer getEventNo() { return eventNo; }
    public String getMallId() { return mallId; }
    public String getShippingCarrierCode() { return shippingCarrierCode; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
