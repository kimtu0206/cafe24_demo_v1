package org.example.cafe24_demo_v1.order.domain.repository;

import org.example.cafe24_demo_v1.order.domain.model.OrderWebhookEvent;
import org.example.cafe24_demo_v1.order.domain.model.OrderWebhookEventStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 Webhook 이벤트(cafe24_order_webhook_event) 저장소 포트(인터페이스).
 *
 * 도메인 레이어에 위치하기 때문에 JPA나 DB에 대한 의존성이 전혀 없다.
 * 실제 구현체(JPA)는 infrastructure 레이어의 OrderWebhookEventRepositoryAdapter가 담당한다.
 */
public interface OrderWebhookEventRepository {

    /** eventNo + mallId + resourceId 조합이 이미 수신된 이벤트인지 확인한다. */
    boolean exists(Integer eventNo, String mallId, String resourceId);

    void save(OrderWebhookEvent event);

    /**
     * 재시도 대상 이벤트를 오래된 순으로 최대 limit건 조회한다. 다음 중 하나에 해당하면 대상이다:
     * (1) RECEIVED 또는 FAILED 상태이면서 nextRetryAt이 now 이전이거나 없음
     * (2) PROCESSING 상태인데 lastTriedAt이 processingStaleBefore 이전(앱 크래시로 추정)
     * DEAD/PROCESSED 상태는 대상에서 제외된다.
     */
    List<OrderWebhookEvent> findRetryableEvents(LocalDateTime now, LocalDateTime processingStaleBefore, int limit);

    /** 주어진 상태의 이벤트 건수를 센다. */
    long countByStatus(OrderWebhookEventStatus status);

    /** 전체 이벤트의 retryCount 합계를 구한다(이벤트가 하나도 없으면 0). */
    long sumRetryCount();
}