package org.example.cafe24_demo_v1.admin.presentation;

import org.example.cafe24_demo_v1.admin.application.BackfillService;
import org.example.cafe24_demo_v1.shared.application.SyncResult;
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AdminBackfillControllerTest {

    @Mock private BackfillService backfillService;

    private AdminBackfillController controller;

    @BeforeEach
    void setUp() {
        Cafe24Properties properties = new Cafe24Properties();
        properties.setMallId("mymall");
        controller = new AdminBackfillController(backfillService, properties);
    }

    @Test
    void backfillOrders는_apiFailureCount가_0이면_200을_반환한다() {
        LocalDate startDate = LocalDate.of(2026, 6, 1);
        LocalDate endDate = LocalDate.of(2026, 6, 24);
        given(backfillService.backfillOrders("mymall", startDate, endDate))
                .willReturn(new SyncResult(10, 1, 0, null));

        ResponseEntity<?> response = controller.backfillOrders(startDate, endDate);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void backfillOrders는_apiFailureCount가_있으면_502를_반환한다() {
        LocalDate startDate = LocalDate.of(2026, 6, 1);
        LocalDate endDate = LocalDate.of(2026, 6, 24);
        given(backfillService.backfillOrders("mymall", startDate, endDate))
                .willReturn(new SyncResult(0, 0, 1, "Cafe24 order API call failed. status=500"));

        ResponseEntity<?> response = controller.backfillOrders(startDate, endDate);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    void backfillProducts는_apiFailureCount가_있으면_502를_반환한다() {
        given(backfillService.backfillProducts("mymall"))
                .willReturn(new SyncResult(0, 0, 1, "Cafe24 product API call failed. status=500"));

        ResponseEntity<?> response = controller.backfillProducts();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    void backfillCarriers는_apiFailureCount가_0이면_200을_반환한다() {
        given(backfillService.backfillCarriers("mymall"))
                .willReturn(new SyncResult(3, 0, 0, null));

        ResponseEntity<?> response = controller.backfillCarriers();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
