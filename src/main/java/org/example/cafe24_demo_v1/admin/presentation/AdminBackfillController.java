package org.example.cafe24_demo_v1.admin.presentation;

import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.admin.application.BackfillService;
import org.example.cafe24_demo_v1.carrier.application.service.CarrierService;
import org.example.cafe24_demo_v1.order.application.service.OrderService;
import org.example.cafe24_demo_v1.product.application.service.ProductService;
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/backfill")
@RequiredArgsConstructor
public class AdminBackfillController {

    private final BackfillService backfillService;
    private final Cafe24Properties cafe24Properties;

    @PostMapping("/orders")
    public ResponseEntity<BackfillResponse> backfillOrders(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        OrderService.SyncResult result = backfillService.backfillOrders(cafe24Properties.getMallId(), startDate, endDate);
        return toResponse("orders", result.processedCount(), result.failedCount(), result.apiFailureCount(), result.errorMessage());
    }

    @PostMapping("/products")
    public ResponseEntity<BackfillResponse> backfillProducts() {
        ProductService.SyncResult result = backfillService.backfillProducts(cafe24Properties.getMallId());
        return toResponse("products", result.processedCount(), result.failedCount(), result.apiFailureCount(), result.errorMessage());
    }

    @PostMapping("/carriers")
    public ResponseEntity<BackfillResponse> backfillCarriers() {
        CarrierService.SyncResult result = backfillService.backfillCarriers(cafe24Properties.getMallId());
        return toResponse("carriers", result.processedCount(), result.failedCount(), result.apiFailureCount(), result.errorMessage());
    }

    /**
     * apiFailureCount > 0(Cafe24 API 호출 자체가 끝까지 성공하지 못함)이면 502로 응답한다 —
     * ProductController가 Cafe24 호출 실패를 502로 변환하는 것과 동일한 컨벤션이다.
     * 개별 건 실패(failedCount)는 전체 백필이 끝까지 진행됐다는 뜻이라 200으로 본다.
     */
    private ResponseEntity<BackfillResponse> toResponse(
            String target, int processedCount, int failedCount, int apiFailureCount, String errorMessage
    ) {
        BackfillResponse body = new BackfillResponse(
                target, apiFailureCount == 0 ? "completed" : "failed",
                processedCount, failedCount, apiFailureCount, errorMessage
        );
        return apiFailureCount == 0
                ? ResponseEntity.ok(body)
                : ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }

    private record BackfillResponse(
            String target, String status, int processedCount, int failedCount, int apiFailureCount, String errorMessage
    ) {}
}
