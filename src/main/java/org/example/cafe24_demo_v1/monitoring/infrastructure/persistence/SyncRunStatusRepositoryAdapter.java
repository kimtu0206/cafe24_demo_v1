package org.example.cafe24_demo_v1.monitoring.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.monitoring.domain.model.SyncRunStatus;
import org.example.cafe24_demo_v1.monitoring.domain.model.SyncTarget;
import org.example.cafe24_demo_v1.monitoring.domain.repository.SyncRunStatusRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class SyncRunStatusRepositoryAdapter implements SyncRunStatusRepository {

    private final SyncRunStatusJpaRepository jpaRepository;
    private final SyncRunStatusMapper mapper;

    @Override
    public SyncRunStatus findOrInit(String mallId, SyncTarget syncTarget) {
        return jpaRepository.findByMallIdAndSyncTarget(mallId, syncTarget.name())
                .map(mapper::toDomain)
                .orElseGet(() -> SyncRunStatus.init(mallId, syncTarget));
    }

    @Override
    public void save(SyncRunStatus status) {
        SyncRunStatusEntity entity = mapper.toEntity(status);
        SyncRunStatusEntity saved = jpaRepository.save(entity);
        status.setId(saved.getId());
    }

    @Override
    public List<SyncRunStatus> findAllByMallId(String mallId) {
        return jpaRepository.findAllByMallId(mallId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
