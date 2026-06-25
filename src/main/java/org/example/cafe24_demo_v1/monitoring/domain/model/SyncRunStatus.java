package org.example.cafe24_demo_v1.monitoring.domain.model;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * mallId + syncTarget 단위로 가장 최근 동기화 실행 결과 1건만 보관하는 도메인 모델.
 *
 * 개별 실행 이력을 누적하지 않고 매번 덮어쓴다(최신 상태 조회가 목적이라 이력 보관은 범위 밖).
 * "성공"의 의미는 Cafe24 API 호출 자체가 끝까지 정상적으로 끝났는지를 뜻하며(apiFailureCount == 0),
 * 개별 항목(order/product/carrier 1건) 처리 실패는 별도로 failedCount에 집계될 뿐 전체 실행을
 * 실패로 보지 않는다 — 한 건의 실패가 나머지 건 처리를 막지 않는 기존 동기화 정책과 일치시키기 위함이다.
 */
@Getter
public class SyncRunStatus {

    private Long id;
    private String mallId;
    private SyncTarget syncTarget;
    private LocalDateTime lastRunAt;
    private LocalDateTime lastSuccessAt;
    private int lastProcessedCount;
    private int lastFailedCount;
    private int lastApiFailureCount;
    private String lastErrorMessage;

    private SyncRunStatus() {}

    /** 아직 한 번도 실행 결과가 기록되지 않은 초기 상태를 생성한다. */
    public static SyncRunStatus init(String mallId, SyncTarget syncTarget) {
        SyncRunStatus status = new SyncRunStatus();
        status.mallId = mallId;
        status.syncTarget = syncTarget;
        status.lastProcessedCount = 0;
        status.lastFailedCount = 0;
        status.lastApiFailureCount = 0;
        return status;
    }

    /** DB에서 조회한 데이터로 도메인 객체를 복원할 때 사용한다. */
    public static SyncRunStatus reconstitute(
            Long id, String mallId, SyncTarget syncTarget, LocalDateTime lastRunAt, LocalDateTime lastSuccessAt,
            int lastProcessedCount, int lastFailedCount, int lastApiFailureCount, String lastErrorMessage
    ) {
        SyncRunStatus status = new SyncRunStatus();
        status.id = id;
        status.mallId = mallId;
        status.syncTarget = syncTarget;
        status.lastRunAt = lastRunAt;
        status.lastSuccessAt = lastSuccessAt;
        status.lastProcessedCount = lastProcessedCount;
        status.lastFailedCount = lastFailedCount;
        status.lastApiFailureCount = lastApiFailureCount;
        status.lastErrorMessage = lastErrorMessage;
        return status;
    }

    /**
     * 동기화 1회 실행 결과를 덮어쓴다. apiFailureCount가 0이면(Cafe24 API 호출 자체는 끝까지
     * 성공) lastSuccessAt을 이번 실행 시각으로 갱신한다.
     */
    public void recordRun(int processedCount, int failedCount, int apiFailureCount, String errorMessage) {
        this.lastRunAt = LocalDateTime.now();
        this.lastProcessedCount = processedCount;
        this.lastFailedCount = failedCount;
        this.lastApiFailureCount = apiFailureCount;
        this.lastErrorMessage = errorMessage;
        if (apiFailureCount == 0) {
            this.lastSuccessAt = this.lastRunAt;
        }
    }

    public void setId(Long id) { this.id = id; }
}
