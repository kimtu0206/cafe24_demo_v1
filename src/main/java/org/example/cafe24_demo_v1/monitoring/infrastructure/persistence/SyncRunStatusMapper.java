package org.example.cafe24_demo_v1.monitoring.infrastructure.persistence;

import org.example.cafe24_demo_v1.monitoring.domain.model.SyncRunStatus;
import org.example.cafe24_demo_v1.monitoring.domain.model.SyncTarget;
import org.springframework.stereotype.Component;

/**
 * 도메인 모델(SyncRunStatus) ↔ JPA 엔티티(SyncRunStatusEntity) 간 변환을 담당하는 매퍼.
 */
@Component
class SyncRunStatusMapper {

    SyncRunStatus toDomain(SyncRunStatusEntity entity) {
        return SyncRunStatus.reconstitute(
                entity.getId(),
                entity.getMallId(),
                SyncTarget.valueOf(entity.getSyncTarget()),
                entity.getLastRunAt(),
                entity.getLastSuccessAt(),
                entity.getLastProcessedCount(),
                entity.getLastFailedCount(),
                entity.getLastApiFailureCount(),
                entity.getLastErrorMessage()
        );
    }

    SyncRunStatusEntity toEntity(SyncRunStatus domain) {
        SyncRunStatusEntity entity = new SyncRunStatusEntity();
        entity.setId(domain.getId());
        entity.setMallId(domain.getMallId());
        entity.setSyncTarget(domain.getSyncTarget().name());
        entity.setLastRunAt(domain.getLastRunAt());
        entity.setLastSuccessAt(domain.getLastSuccessAt());
        entity.setLastProcessedCount(domain.getLastProcessedCount());
        entity.setLastFailedCount(domain.getLastFailedCount());
        entity.setLastApiFailureCount(domain.getLastApiFailureCount());
        entity.setLastErrorMessage(domain.getLastErrorMessage());
        return entity;
    }
}
