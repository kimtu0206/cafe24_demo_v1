package org.example.cafe24_demo_v1.shared.application;

import org.example.cafe24_demo_v1.monitoring.application.service.SyncMetricsService;
import org.example.cafe24_demo_v1.monitoring.domain.model.SyncTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SyncFailureRecorderTest {

    @Mock private SyncMetricsService syncMetricsService;

    private SyncFailureRecorder syncFailureRecorder;

    @BeforeEach
    void setUp() {
        syncFailureRecorder = new SyncFailureRecorder(syncMetricsService);
    }

    @Test
    void recordAuthFailure는_메트릭을_기록하고_실패_SyncResult를_반환한다() {
        Exception e = new IllegalStateException("Authorization not found: mymall");

        SyncResult result = syncFailureRecorder.recordAuthFailure("mymall", SyncTarget.ORDER, e);

        assertThat(result.apiFailureCount()).isEqualTo(1);
        assertThat(result.processedCount()).isZero();
        assertThat(result.failedCount()).isZero();
        assertThat(result.errorMessage()).isEqualTo(e.getMessage());
        verify(syncMetricsService).recordRun("mymall", SyncTarget.ORDER, 0, 0, 1, e.getMessage());
    }

    @Test
    void recordAuthFailure는_SyncTarget별로_각_타깃에_메트릭을_기록한다() {
        Exception e = new IllegalStateException("auth error");

        syncFailureRecorder.recordAuthFailure("mymall", SyncTarget.PRODUCT, e);
        verify(syncMetricsService).recordRun("mymall", SyncTarget.PRODUCT, 0, 0, 1, e.getMessage());

        syncFailureRecorder.recordAuthFailure("mymall", SyncTarget.CARRIER, e);
        verify(syncMetricsService).recordRun("mymall", SyncTarget.CARRIER, 0, 0, 1, e.getMessage());
    }
}
