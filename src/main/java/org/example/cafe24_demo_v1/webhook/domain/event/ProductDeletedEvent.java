package org.example.cafe24_demo_v1.webhook.domain.event;

import java.time.LocalDateTime;

/**
 * Cafe24 쇼핑몰에서 상품이 삭제됐을 때 발행되는 도메인 이벤트.
 *
 * WebhookController가 Cafe24로부터 상품 삭제 Webhook을 수신하면 이 이벤트를 발행한다.
 * WebhookEventService가 이 이벤트를 구독해 ProductService로 로컬 DB에서도 상품을 삭제한다.
 */
public class ProductDeletedEvent {

    private final Integer eventNo;   // Cafe24 이벤트 번호 (중복 수신 판단에 사용)
    private final String mallId;     // 상품이 삭제된 쇼핑몰 ID
    private final Long productNo;    // 삭제된 상품 번호
    private final LocalDateTime occurredAt;

    public ProductDeletedEvent(Integer eventNo, String mallId, Long productNo) {
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