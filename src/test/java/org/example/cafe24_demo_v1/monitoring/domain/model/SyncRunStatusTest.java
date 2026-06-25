package org.example.cafe24_demo_v1.monitoring.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SyncRunStatusTest {

    @Test
    void init은_실행_기록이_없는_초기_상태를_만든다() {
        SyncRunStatus status = SyncRunStatus.init("mymall", SyncTarget.ORDER);

        assertThat(status.getMallId()).isEqualTo("mymall");
        assertThat(status.getSyncTarget()).isEqualTo(SyncTarget.ORDER);
        assertThat(status.getLastRunAt()).isNull();
        assertThat(status.getLastSuccessAt()).isNull();
        assertThat(status.getLastProcessedCount()).isZero();
    }

    @Test
    void recordRun은_API_실패가_없으면_마지막_성공_시각을_갱신한다() {
        SyncRunStatus status = SyncRunStatus.init("mymall", SyncTarget.ORDER);

        status.recordRun(10, 1, 0, null);

        assertThat(status.getLastRunAt()).isNotNull();
        assertThat(status.getLastSuccessAt()).isEqualTo(status.getLastRunAt());
        assertThat(status.getLastProcessedCount()).isEqualTo(10);
        assertThat(status.getLastFailedCount()).isEqualTo(1);
        assertThat(status.getLastApiFailureCount()).isZero();
        assertThat(status.getLastErrorMessage()).isNull();
    }

    @Test
    void recordRun은_API_실패가_있으면_마지막_성공_시각을_갱신하지_않는다() {
        SyncRunStatus status = SyncRunStatus.init("mymall", SyncTarget.ORDER);
        status.recordRun(10, 0, 0, null);
        var previousSuccessAt = status.getLastSuccessAt();

        status.recordRun(3, 0, 1, "Cafe24 API 호출 실패");

        assertThat(status.getLastRunAt()).isNotNull();
        assertThat(status.getLastSuccessAt()).isEqualTo(previousSuccessAt);
        assertThat(status.getLastApiFailureCount()).isEqualTo(1);
        assertThat(status.getLastErrorMessage()).isEqualTo("Cafe24 API 호출 실패");
    }
}
