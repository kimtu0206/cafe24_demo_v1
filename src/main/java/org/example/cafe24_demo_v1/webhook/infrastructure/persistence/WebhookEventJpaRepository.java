package org.example.cafe24_demo_v1.webhook.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Webhook 이벤트 이력 JPA 레포지토리.
 */
interface WebhookEventJpaRepository extends JpaRepository<WebhookEventEntity, Long> {

    /** eventNo + mallId 조합으로 이미 처리된 이벤트인지 확인한다. */
    boolean existsByEventNoAndMallId(Integer eventNo, String mallId);
}
