package org.example.cafe24_demo_v1.monitoring.domain.repository;

import org.example.cafe24_demo_v1.monitoring.domain.model.SyncRunStatus;
import org.example.cafe24_demo_v1.monitoring.domain.model.SyncTarget;

import java.util.List;

/**
 * 동기화 실행 결과(sync_run_status) 저장소 포트(인터페이스).
 *
 * 도메인 레이어에 위치하기 때문에 JPA나 DB에 대한 의존성이 전혀 없다.
 * 실제 구현체(JPA)는 infrastructure 레이어의 SyncRunStatusRepositoryAdapter가 담당한다.
 */
public interface SyncRunStatusRepository {

    /** 기존 기록이 있으면 조회해서, 없으면 초기 상태(SyncRunStatus.init)를 새로 만들어 반환한다. */
    SyncRunStatus findOrInit(String mallId, SyncTarget syncTarget);

    void save(SyncRunStatus status);

    /** mallId 기준으로 모든 syncTarget의 최신 실행 결과를 조회한다. */
    List<SyncRunStatus> findAllByMallId(String mallId);
}
