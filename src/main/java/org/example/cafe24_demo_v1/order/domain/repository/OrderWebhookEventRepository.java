package org.example.cafe24_demo_v1.order.domain.repository;

import org.example.cafe24_demo_v1.order.domain.model.OrderWebhookEvent;

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

    /** 미처리(processed=false) 이벤트를 오래된 순으로 최대 limit건 조회한다. */
    List<OrderWebhookEvent> findUnprocessed(int limit);
}