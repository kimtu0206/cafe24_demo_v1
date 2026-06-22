package org.example.cafe24_demo_v1.order.domain.model;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 수신한 주문 생성 Webhook의 원본 데이터와 처리 상태를 담는 도메인 모델.
 *
 * Webhook 수신 시점에는 원본 payload만 저장하고(receive), 실제 주문 테이블 반영은
 * OrderWebhookEventProcessor가 비동기로 처리한 뒤 markProcessed/markFailed로 상태를 갱신한다.
 */
@Getter
public class OrderWebhookEvent {

    private Long id;
    private String mallId;
    private Integer eventNo;      // Cafe24 이벤트 번호 (중복 수신 판단에 사용)
    private String eventType;     // 이벤트 종류를 사람이 읽을 수 있게 보조하는 값 (예: ORDER_CREATED)
    private String resourceId;    // Cafe24 주문번호(order_id)
    private String webhookId;     // Cafe24가 제공하면 채워짐. 현재 payload에는 없어 항상 null
    private String payload;       // Webhook 원본 JSON(resource)
    private LocalDateTime receivedAt;
    private boolean processed;
    private LocalDateTime processedAt;
    private String errorMessage;
    private LocalDateTime createdAt;

    private OrderWebhookEvent() {}

    /** Webhook을 막 수신했을 때 미처리 상태로 생성한다. */
    public static OrderWebhookEvent receive(
            String mallId, Integer eventNo, String eventType, String resourceId, String webhookId, String payload
    ) {
        OrderWebhookEvent event = new OrderWebhookEvent();
        event.mallId = mallId;
        event.eventNo = eventNo;
        event.eventType = eventType;
        event.resourceId = resourceId;
        event.webhookId = webhookId;
        event.payload = payload;
        event.receivedAt = LocalDateTime.now();
        event.processed = false;
        event.createdAt = LocalDateTime.now();
        return event;
    }

    /** DB에서 조회한 데이터로 도메인 객체를 복원할 때 사용한다. */
    public static OrderWebhookEvent reconstitute(
            Long id, String mallId, Integer eventNo, String eventType, String resourceId, String webhookId,
            String payload, LocalDateTime receivedAt, boolean processed, LocalDateTime processedAt,
            String errorMessage, LocalDateTime createdAt
    ) {
        OrderWebhookEvent event = new OrderWebhookEvent();
        event.id = id;
        event.mallId = mallId;
        event.eventNo = eventNo;
        event.eventType = eventType;
        event.resourceId = resourceId;
        event.webhookId = webhookId;
        event.payload = payload;
        event.receivedAt = receivedAt;
        event.processed = processed;
        event.processedAt = processedAt;
        event.errorMessage = errorMessage;
        event.createdAt = createdAt;
        return event;
    }

    /** 주문 테이블 반영에 성공했을 때 호출한다. */
    public void markProcessed() {
        this.processed = true;
        this.processedAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    /** 주문 테이블 반영에 실패했을 때 호출한다. processed는 그대로 false로 남아 다음 주기에 재시도된다. */
    public void markFailed(String errorMessage) {
        this.processed = false;
        this.errorMessage = errorMessage;
    }

    public void setId(Long id) { this.id = id; }
}
