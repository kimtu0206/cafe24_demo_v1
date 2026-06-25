package org.example.cafe24_demo_v1.monitoring.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DB 테이블(sync_run_status)과 매핑되는 JPA 엔티티.
 * mallId + syncTarget 단위로 가장 최근 동기화 실행 결과 1건만 보관한다(이력 누적 아님).
 */
@Entity
@Table(
        name = "sync_run_status",
        uniqueConstraints = @UniqueConstraint(columnNames = {"mall_id", "sync_target"})
)
@Getter
@Setter
class SyncRunStatusEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mall_id", nullable = false)
    private String mallId;

    @Column(name = "sync_target", nullable = false)
    private String syncTarget; // SyncTarget.name() — ORDER/PRODUCT/CARRIER

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    @Column(name = "last_success_at")
    private LocalDateTime lastSuccessAt;

    @Column(name = "last_processed_count", nullable = false)
    private int lastProcessedCount;

    @Column(name = "last_failed_count", nullable = false)
    private int lastFailedCount;

    @Column(name = "last_api_failure_count", nullable = false)
    private int lastApiFailureCount;

    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;
}
