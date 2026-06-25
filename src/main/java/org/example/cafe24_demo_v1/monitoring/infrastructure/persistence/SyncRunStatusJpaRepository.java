package org.example.cafe24_demo_v1.monitoring.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface SyncRunStatusJpaRepository extends JpaRepository<SyncRunStatusEntity, Long> {

    Optional<SyncRunStatusEntity> findByMallIdAndSyncTarget(String mallId, String syncTarget);

    List<SyncRunStatusEntity> findAllByMallId(String mallId);
}
