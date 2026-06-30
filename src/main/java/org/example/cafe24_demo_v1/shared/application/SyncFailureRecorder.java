package org.example.cafe24_demo_v1.shared.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.monitoring.application.service.SyncMetricsService;
import org.example.cafe24_demo_v1.monitoring.domain.model.SyncTarget;
import org.springframework.stereotype.Component;

/**
 * 주기 동기화(syncFromCafe24) 중 인증 실패를 일관되게 처리하는 헬퍼.
 * Order/Product/Carrier 서비스에서 동일하게 반복되던 "로그 + 메트릭 기록 + 실패 SyncResult 반환" 패턴을 통합한다.
 * backfill처럼 메트릭을 기록하지 않는 경우에는 이 헬퍼를 사용하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncFailureRecorder {

    private final SyncMetricsService syncMetricsService;

    /** 인증 실패를 로그에 남기고 메트릭에 기록한 뒤 실패 SyncResult를 반환한다. */
    public SyncResult recordAuthFailure(String mallId, SyncTarget target, Exception e) {
        log.error("{} sync 인증 실패, 이번 실행 중단: mallId={}", target, mallId, e);
        syncMetricsService.recordRun(mallId, target, 0, 0, 1, e.getMessage());
        return new SyncResult(0, 0, 1, e.getMessage());
    }
}
