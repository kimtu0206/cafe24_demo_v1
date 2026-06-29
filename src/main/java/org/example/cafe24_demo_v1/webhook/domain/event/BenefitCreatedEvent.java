package org.example.cafe24_demo_v1.webhook.domain.event;

import java.time.LocalDateTime;

/**
 * Cafe24 쇼핑몰에 새 혜택이 등록됐을 때 발행되는 도메인 이벤트.
 *
 * BenefitWebhookController가 Cafe24로부터 혜택 등록 Webhook을 수신하면 이 이벤트를 발행한다.
 * WebhookEventService가 이 이벤트를 구독해 BenefitService로 상세 정보를 다시 조회/저장한다.
 */
public class BenefitCreatedEvent {

    private final Integer eventNo;
    private final String mallId;
    private final Integer benefitNo;
    private final LocalDateTime occurredAt;

    public BenefitCreatedEvent(Integer eventNo, String mallId, Integer benefitNo) {
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
