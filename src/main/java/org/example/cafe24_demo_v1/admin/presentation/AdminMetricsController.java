package org.example.cafe24_demo_v1.admin.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.admin.application.MetricsService;
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin")
@RestController
@RequestMapping("/admin/metrics")
@RequiredArgsConstructor
public class AdminMetricsController {

    private final MetricsService metricsService;
    private final Cafe24Properties cafe24Properties;

    @Operation(summary = "운영 지표 조회", description = "Webhook 처리 현황(미처리/FAILED/DEAD 건수, 재시도 누적)과 동기화 대상별 최신 실행 상태를 반환합니다.")
    @GetMapping
    public ResponseEntity<MetricsService.OperationalMetrics> getMetrics() {
        return ResponseEntity.ok(metricsService.getMetrics(cafe24Properties.getMallId()));
    }
}
