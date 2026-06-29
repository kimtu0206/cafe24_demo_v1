package org.example.cafe24_demo_v1.benefit.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface BenefitWebhookEventJpaRepository extends JpaRepository<BenefitWebhookEventEntity, Long> {

    boolean existsByEventNoAndMallIdAndResourceId(Integer eventNo, String mallId, String resourceId);
}
