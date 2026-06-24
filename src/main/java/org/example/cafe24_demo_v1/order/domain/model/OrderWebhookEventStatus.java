package org.example.cafe24_demo_v1.order.domain.model;

/**
 * 주문 Webhook 이벤트의 처리 상태.
 *
 * RECEIVED: 수신 후 아직 처리되지 않음(최초 상태)
 * PROCESSED: order 테이블 반영 성공
 * FAILED: 처리 실패. nextRetryAt 이후 재시도 대상이 됨
 * DEAD: 재시도 횟수(MAX_RETRY_COUNT) 초과로 더 이상 재시도하지 않음
 */
public enum OrderWebhookEventStatus {
    RECEIVED,   // 수신됨
    PROCESSED,  // 성공
    FAILED,     // 실패
    DEAD        // 재시도 실패
}
