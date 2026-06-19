package org.example.cafe24_demo_v1.webhook.domain.repository;

/**
 * Webhook 이벤트 이력 저장소 인터페이스.
 *
 * 도메인 레이어에 위치하기 때문에 JPA나 DB에 대한 의존성이 전혀 없다.
 * 실제 구현체(JPA)는 infrastructure 레이어의 WebhookEventRepositoryAdapter가 담당한다.
 */
public interface WebhookEventRepository {

    /**
     * eventNo + mallId + resourceId 조합이 이미 처리된 이벤트인지 확인한다.
     * resourceId는 리소스(예: 상품)가 없는 이벤트라면 null을 넘기면 된다.
     */
    boolean exists(Integer eventNo, String mallId, String resourceId);

    /** 수신한 이벤트를 이력에 저장한다. */
    void save(Integer eventNo, String mallId, String resourceId);
}
