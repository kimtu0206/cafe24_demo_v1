package org.example.cafe24_demo_v1.order.domain.model;

import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 수신한 주문 생성 Webhook의 원본 데이터와 처리 상태를 담는 도메인 모델.
 *
 * Webhook 수신 시점에는 원본 payload만 저장하고(receive), 실제 주문 테이블 반영은
 * OrderWebhookEventProcessor가 비동기로 처리한 뒤 markProcessed/markFailed로 상태를 갱신한다.
 */
@Getter
public class OrderWebhookEvent {

    /** 실패가 이 횟수를 넘으면 더 이상 재시도하지 않고 DEAD로 전환한다. */
    private static final int MAX_RETRY_COUNT = 5;
    /** 실패한 이벤트를 같은 배치 조회에서 바로 다시 집지 않도록 두는 재시도 간격. */
    private static final Duration RETRY_BACKOFF = Duration.ofHours(1);
    /**
     * PROCESSING 상태로 머문 시간이 이 값을 넘으면 크래시로 보고 재시도 대상에 다시 포함시킨다.
     * Cafe24OrderClient의 connect/read timeout(3s+5s) 합보다 충분히 길게 잡아, 정상 진행 중인
     * 호출을 재수집하지 않으면서도 앱이 죽어 영구히 PROCESSING으로 남는 것은 방지한다.
     */
    public static final Duration PROCESSING_STALE_TIMEOUT = Duration.ofMinutes(5);

    private Long id;
    private String mallId;
    private Integer eventNo;      // Cafe24 이벤트 번호 (중복 수신 판단에 사용)
    private String eventType;     // 이벤트 종류를 사람이 읽을 수 있게 보조하는 값 (예: ORDER_CREATED)
    private String resourceId;    // Cafe24 주문번호(order_id)
    private String webhookId;     // Cafe24가 제공하면 채워짐. 현재 payload에는 없어 항상 null
    private String payload;       // Webhook 원본 JSON(resource)
    private LocalDateTime receivedAt;
    private OrderWebhookEventStatus status;
    private int retryCount;
    private LocalDateTime nextRetryAt;
    private LocalDateTime lastTriedAt;
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
        event.status = OrderWebhookEventStatus.RECEIVED;
        event.retryCount = 0;
        event.createdAt = LocalDateTime.now();
        return event;
    }

    /** DB에서 조회한 데이터로 도메인 객체를 복원할 때 사용한다. */
    public static OrderWebhookEvent reconstitute(
            Long id, String mallId, Integer eventNo, String eventType, String resourceId, String webhookId,
            String payload, LocalDateTime receivedAt, OrderWebhookEventStatus status, int retryCount,
            LocalDateTime nextRetryAt, LocalDateTime lastTriedAt, LocalDateTime processedAt, String errorMessage,
            LocalDateTime createdAt
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
        event.status = status;
        event.retryCount = retryCount;
        event.nextRetryAt = nextRetryAt;
        event.lastTriedAt = lastTriedAt;
        event.processedAt = processedAt;
        event.errorMessage = errorMessage;
        event.createdAt = createdAt;
        return event;
    }

    /** Cafe24 재조회를 시도하기 직전에 호출한다. 시도 시각을 기록해 크래시 시 복구 기준으로 쓴다. */
    public void markProcessing() {
        this.status = OrderWebhookEventStatus.PROCESSING;
        this.lastTriedAt = LocalDateTime.now();
    }

    /** 주문 테이블 반영에 성공했을 때 호출한다. */
    public void markProcessed() {
        this.status = OrderWebhookEventStatus.PROCESSED;
        this.processedAt = LocalDateTime.now();
        this.nextRetryAt = null;
        this.errorMessage = null;
    }

    /**
     * 주문 테이블 반영에 실패했을 때 호출한다.
     * retryCount를 늘리고 nextRetryAt을 미래로 미뤄, 같은 배치 조회에서 바로 다시 집히지 않게 한다.
     * MAX_RETRY_COUNT를 넘으면 더 이상 재시도하지 않는 DEAD로 전환한다.
     */
    public void markFailed(String errorMessage) {
        this.retryCount++;
        this.status = this.retryCount >= MAX_RETRY_COUNT
                ? OrderWebhookEventStatus.DEAD
                : OrderWebhookEventStatus.FAILED;
        this.nextRetryAt = LocalDateTime.now().plus(RETRY_BACKOFF);
        this.errorMessage = errorMessage;
    }

    public boolean isProcessed() {
        return this.status == OrderWebhookEventStatus.PROCESSED;
    }

    public void setId(Long id) { this.id = id; }
}
