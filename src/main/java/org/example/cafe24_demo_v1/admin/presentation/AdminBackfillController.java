package org.example.cafe24_demo_v1.admin.presentation;

import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.admin.application.BackfillService;
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.springframework.format.annotation.DateTimeFormat;
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
        backfillService.backfillOrders(cafe24Properties.getMallId(), startDate, endDate);
        return ResponseEntity.ok(new BackfillResponse("orders", "completed"));
    }

    @PostMapping("/products")
    public ResponseEntity<BackfillResponse> backfillProducts() {
        backfillService.backfillProducts(cafe24Properties.getMallId());
        return ResponseEntity.ok(new BackfillResponse("products", "completed"));
    }

    @PostMapping("/carriers")
    public ResponseEntity<BackfillResponse> backfillCarriers() {
        backfillService.backfillCarriers(cafe24Properties.getMallId());
        return ResponseEntity.ok(new BackfillResponse("carriers", "completed"));
    }

    private record BackfillResponse(String target, String status) {}
}
