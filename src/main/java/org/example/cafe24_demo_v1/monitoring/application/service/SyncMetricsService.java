package org.example.cafe24_demo_v1.monitoring.application.service;

import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.monitoring.domain.model.SyncRunStatus;
import org.example.cafe24_demo_v1.monitoring.domain.model.SyncTarget;
import org.example.cafe24_demo_v1.monitoring.domain.repository.SyncRunStatusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * order/product/carrier 각 동기화가 끝날 때마다 실행 결과를 기록하고,
 * mallId 기준으로 모든 동기화 대상의 최신 상태를 조회하는 애플리케이션 서비스.
 *
 * 순수 DB 조작만 수행하므로(Cafe24 외부 호출 없음) @Transactional로 감싼다.
 */
@Service
@RequiredArgsConstructor
public class SyncMetricsService {

    private final SyncRunStatusRepository repository;

    @Transactional
    public void recordRun(String mallId, SyncTarget target, int processedCount, int failedCount, int apiFailureCount, String errorMessage) {
        SyncRunStatus status = repository.findOrInit(mallId, target);
        status.recordRun(processedCount, failedCount, apiFailureCount, errorMessage);
        repository.save(status);
    }

    public List<SyncRunStatus> getAll(String mallId) {
        return repository.findAllByMallId(mallId);
    }
}
