package org.example.cafe24_demo_v1.order.infrastructure.persistence;

import org.example.cafe24_demo_v1.order.domain.model.OrderWebhookEvent;
import org.example.cafe24_demo_v1.order.domain.model.OrderWebhookEventStatus;
import org.springframework.stereotype.Component;

/**
 * 도메인 모델(OrderWebhookEvent) ↔ JPA 엔티티(OrderWebhookEventEntity) 간 변환을 담당하는 매퍼.
 */
@Component
class OrderWebhookEventMapper {

    OrderWebhookEvent toDomain(OrderWebhookEventEntity entity) {
        return OrderWebhookEvent.reconstitute(
                entity.getId(),
                entity.getMallId(),
                entity.getEventNo(),
                entity.getEventType(),
                entity.getResourceId(),
                entity.getWebhookId(),
                entity.getPayload(),
                entity.getReceivedAt(),
                OrderWebhookEventStatus.valueOf(entity.getStatus()),
                entity.getRetryCount(),
                entity.getNextRetryAt(),
                entity.getLastTriedAt(),
                entity.getProcessedAt(),
                entity.getErrorMessage(),
                entity.getCreatedAt()
        );
    }

    OrderWebhookEventEntity toEntity(OrderWebhookEvent domain) {
        OrderWebhookEventEntity entity = new OrderWebhookEventEntity();
        entity.setId(domain.getId());
        entity.setMallId(domain.getMallId());
        entity.setEventNo(domain.getEventNo());
        entity.setEventType(domain.getEventType());
        entity.setResourceId(domain.getResourceId());
        entity.setWebhookId(domain.getWebhookId());
        entity.setPayload(domain.getPayload());
        entity.setReceivedAt(domain.getReceivedAt());
        entity.setStatus(domain.getStatus().name());
        entity.setRetryCount(domain.getRetryCount());
        entity.setNextRetryAt(domain.getNextRetryAt());
        entity.setLastTriedAt(domain.getLastTriedAt());
        entity.setProcessedAt(domain.getProcessedAt());
        entity.setErrorMessage(domain.getErrorMessage());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}
