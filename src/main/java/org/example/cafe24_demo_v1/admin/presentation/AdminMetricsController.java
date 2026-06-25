package org.example.cafe24_demo_v1.admin.presentation;

import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.admin.application.MetricsService;
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/metrics")
@RequiredArgsConstructor
public class AdminMetricsController {

    private final MetricsService metricsService;
    private final Cafe24Properties cafe24Properties;

    @GetMapping
    public ResponseEntity<MetricsService.OperationalMetrics> getMetrics() {
        return ResponseEntity.ok(metricsService.getMetrics(cafe24Properties.getMallId()));
    }
}
