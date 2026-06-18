package org.example.cafe24_demo_v1.webhook.domain.event;

import java.time.LocalDateTime;

/**
 * Cafe24 쇼핑몰에서 상품이 수정됐을 때 발행되는 도메인 이벤트.
 *
 * WebhookController가 Cafe24로부터 상품 수정 Webhook을 수신하면 이 이벤트를 발행한다.
 * WebhookEventService가 이 이벤트를 구독해 ProductService로 상세 정보를 다시 조회/저장한다.
 */
public class ProductUpdatedEvent {

    private final Integer eventNo;   // Cafe24 이벤트 번호 (중복 수신 판단에 사용)
    private final String mallId;     // 상품이 수정된 쇼핑몰 ID
    private final Long productNo;    // 수정된 상품 번호
    private final LocalDateTime occurredAt;

    public ProductUpdatedEvent(Integer eventNo, String mallId, Long productNo) {
        this.eventNo = eventNo;
        this.mallId = mallId;
        this.productNo = productNo;
        this.occurredAt = LocalDateTime.now();
    }

    public Integer getEventNo() { return eventNo; }
    public String getMallId() { return mallId; }
    public Long getProductNo() { return productNo; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}