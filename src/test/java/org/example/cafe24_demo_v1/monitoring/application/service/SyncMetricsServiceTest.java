package org.example.cafe24_demo_v1.monitoring.application.service;

import org.example.cafe24_demo_v1.monitoring.domain.model.SyncRunStatus;
import org.example.cafe24_demo_v1.monitoring.domain.model.SyncTarget;
import org.example.cafe24_demo_v1.monitoring.domain.repository.SyncRunStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SyncMetricsServiceTest {

    @Mock private SyncRunStatusRepository repository;

    private SyncMetricsService service;

    @BeforeEach
    void setUp() {
        service = new SyncMetricsService(repository);
    }

    @Test
    void recordRun은_조회한_상태에_실행_결과를_기록해_저장한다() {
        SyncRunStatus status = SyncRunStatus.init("mymall", SyncTarget.ORDER);
        given(repository.findOrInit("mymall", SyncTarget.ORDER)).willReturn(status);

        service.recordRun("mymall", SyncTarget.ORDER, 10, 1, 0, null);

        assertThat(status.getLastProcessedCount()).isEqualTo(10);
        assertThat(status.getLastFailedCount()).isEqualTo(1);
        verify(repository).save(status);
    }

    @Test
    void getAll은_저장소가_반환한_목록을_그대로_전달한다() {
        List<SyncRunStatus> statuses = List.of(SyncRunStatus.init("mymall", SyncTarget.ORDER));
        given(repository.findAllByMallId("mymall")).willReturn(statuses);

        assertThat(service.getAll("mymall")).isEqualTo(statuses);
    }
}
