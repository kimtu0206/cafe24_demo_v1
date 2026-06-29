package org.example.cafe24_demo_v1.benefit.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.benefit.domain.repository.BenefitWebhookEventRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BenefitWebhookEventRepositoryAdapter implements BenefitWebhookEventRepository {

    private final BenefitWebhookEventJpaRepository jpaRepository;

    @Override
    public boolean exists(Integer eventNo, String mallId, String resourceId) {
        return jpaRepository.existsByEventNoAndMallIdAndResourceId(eventNo, mallId, normalize(resourceId));
    }

    @Override
    public void save(Integer eventNo, String eventType, String mallId, String resourceId) {
        BenefitWebhookEventEntity entity = new BenefitWebhookEventEntity();
        entity.setEventNo(eventNo);
        entity.setEventType(eventType);
        entity.setMallId(mallId);
        entity.setResourceId(normalize(resourceId));
        jpaRepository.save(entity);
    }

    private String normalize(String resourceId) {
        return resourceId == null ? "" : resourceId;
    }
}
