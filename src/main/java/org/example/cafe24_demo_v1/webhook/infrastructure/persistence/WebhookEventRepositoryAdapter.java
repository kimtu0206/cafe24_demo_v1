package org.example.cafe24_demo_v1.webhook.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.webhook.domain.model.WebhookEventType;
import org.example.cafe24_demo_v1.webhook.domain.repository.WebhookEventRepository;
import org.springframework.stereotype.Repository;

/**
 * 도메인 인터페이스(WebhookEventRepository)의 JPA 구현체.
 * 중복 수신 여부 확인과 이력 저장을 담당한다.
 */
@Repository
@RequiredArgsConstructor
public class WebhookEventRepositoryAdapter implements WebhookEventRepository {

    private final WebhookEventJpaRepository jpaRepository;

    @Override
    public boolean exists(Integer eventNo, String mallId, String resourceId) {
        return jpaRepository.existsByEventNoAndMallIdAndResourceId(eventNo, mallId, normalize(resourceId));
    }

    @Override
    public void save(Integer eventNo, WebhookEventType eventType, String mallId, String resourceId) {
        WebhookEventEntity entity = new WebhookEventEntity();
        entity.setEventNo(eventNo);
        entity.setEventType(eventType.name());
        entity.setMallId(mallId);
        entity.setResourceId(normalize(resourceId));
        jpaRepository.save(entity);
    }

    private String normalize(String resourceId) {
        return resourceId == null ? "" : resourceId;
    }
}
