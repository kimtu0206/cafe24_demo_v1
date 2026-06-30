package org.example.cafe24_demo_v1.webhook.domain.event;

import java.time.LocalDateTime;

/**
 * Cafe24 쇼핑몰에서 혜택이 삭제됐을 때 발행되는 도메인 이벤트.
 *
 * BenefitWebhookController가 Cafe24로부터 혜택 삭제 Webhook을 수신하면 이 이벤트를 발행한다.
 * WebhookEventService가 이 이벤트를 구독해 로컬 DB에서 혜택을 제거한다.
 * Cafe24에 이미 삭제된 상태이므로 Cafe24 API를 다시 호출하지 않는다.
 */
public class BenefitDeletedEvent {

    private final Integer eventNo;
    private final String mallId;
    private final Integer benefitNo;
    private final LocalDateTime occurredAt;

    public BenefitDeletedEvent(Integer eventNo, String mallId, Integer benefitNo) {
        this.eventNo = eventNo;
        this.mallId = mallId;
        this.benefitNo = benefitNo;
        this.occurredAt = LocalDateTime.now();
    }

    public Integer getEventNo() { return eventNo; }
    public String getMallId() { return mallId; }
    public Integer getBenefitNo() { return benefitNo; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
