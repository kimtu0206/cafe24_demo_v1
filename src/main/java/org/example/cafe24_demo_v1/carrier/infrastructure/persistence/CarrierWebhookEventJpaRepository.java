package org.example.cafe24_demo_v1.carrier.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 배송사 Webhook 이벤트 이력 JPA 레포지토리.
 */
interface CarrierWebhookEventJpaRepository extends JpaRepository<CarrierWebhookEventEntity, Long> {

    /** eventNo + mallId + resourceId 조합으로 이미 처리된 이벤트인지 확인한다. */
    boolean existsByEventNoAndMallIdAndResourceId(Integer eventNo, String mallId, String resourceId);
}
