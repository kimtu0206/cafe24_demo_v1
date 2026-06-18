package org.example.cafe24_demo_v1.webhook.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Webhook 이벤트 이력 저장소.
 * 중복 수신 여부 확인과 이력 저장을 담당한다.
 */
@Repository
@RequiredArgsConstructor
public class WebhookEventRepository {

    private final WebhookEventJpaRepository jpaRepository;

    /**
     * eventNo + mallId + resourceId 조합이 이미 처리된 이벤트인지 확인한다.
     * resourceId는 리소스(예: 상품)가 없는 이벤트라면 null을 넘기면 된다.
     */
    public boolean exists(Integer eventNo, String mallId, String resourceId) {
        return jpaRepository.existsByEventNoAndMallIdAndResourceId(eventNo, mallId, normalize(resourceId));
    }

    /** 수신한 이벤트를 이력에 저장한다. */
    public void save(Integer eventNo, String mallId, String resourceId) {
        WebhookEventEntity entity = new WebhookEventEntity();
        entity.setEventNo(eventNo);
        entity.setMallId(mallId);
        entity.setResourceId(normalize(resourceId));
        jpaRepository.save(entity);
    }

    private String normalize(String resourceId) {
        return resourceId == null ? "" : resourceId;
    }
}
