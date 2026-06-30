package org.example.cafe24_demo_v1.admin.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.admin.application.BackfillService;
import org.example.cafe24_demo_v1.shared.application.SyncResult;
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "Admin", description = "운영용 Backfill · 지표 · Webhook 재처리 API")
@RestController
@RequestMapping("/admin/backfill")
@RequiredArgsConstructor
public class AdminBackfillController {

    private final BackfillService backfillService;
    private final Cafe24Properties cafe24Properties;

    @Operation(summary = "주문 Backfill", description = "지정 기간의 주문을 Cafe24에서 재조회해 로컬 DB에 반영합니다. Cafe24 API 호출 실패 시 502를 반환합니다.")
    @PostMapping("/orders")
    public ResponseEntity<BackfillResponse> backfillOrders(
            @Parameter(description = "조회 시작일 (yyyy-MM-dd)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "조회 종료일 (yyyy-MM-dd)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        SyncResult result = backfillService.backfillOrders(cafe24Properties.getMallId(), startDate, endDate);
        return toResponse("orders", result.processedCount(), result.failedCount(), result.apiFailureCount(), result.errorMessage());
    }

    @Operation(summary = "상품 Backfill", description = "Cafe24에서 전체 상품을 재조회해 로컬 DB에 반영합니다. Cafe24 API 호출 실패 시 502를 반환합니다.")
    @PostMapping("/products")
    public ResponseEntity<BackfillResponse> backfillProducts() {
        SyncResult result = backfillService.backfillProducts(cafe24Properties.getMallId());
        return toResponse("products", result.processedCount(), result.failedCount(), result.apiFailureCount(), result.errorMessage());
    }

    @Operation(summary = "배송사 Backfill", description = "Cafe24에서 전체 배송사를 재조회해 로컬 DB에 반영합니다. Cafe24 API 호출 실패 시 502를 반환합니다.")
    @PostMapping("/carriers")
    public ResponseEntity<BackfillResponse> backfillCarriers() {
        SyncResult result = backfillService.backfillCarriers(cafe24Properties.getMallId());
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
