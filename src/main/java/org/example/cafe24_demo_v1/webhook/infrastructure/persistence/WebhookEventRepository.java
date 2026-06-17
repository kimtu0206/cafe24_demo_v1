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

    /** eventNo + mallId 조합이 이미 처리된 이벤트인지 확인한다. */
    public boolean existsByEventNoAndMallId(Integer eventNo, String mallId) {
        return jpaRepository.existsByEventNoAndMallId(eventNo, mallId);
    }

    /** 수신한 이벤트를 이력에 저장한다. */
    public void save(Integer eventNo, String mallId) {
        WebhookEventEntity entity = new WebhookEventEntity();
        entity.setEventNo(eventNo);
        entity.setMallId(mallId);
        jpaRepository.save(entity);
    }
}
