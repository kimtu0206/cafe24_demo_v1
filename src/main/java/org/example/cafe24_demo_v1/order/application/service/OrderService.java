package org.example.cafe24_demo_v1.order.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.authorization.application.service.AppAuthorizationService;
import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.monitoring.application.service.SyncMetricsService;
import org.example.cafe24_demo_v1.monitoring.domain.model.SyncTarget;
import org.example.cafe24_demo_v1.order.domain.model.Order;
import org.example.cafe24_demo_v1.order.domain.repository.OrderRepository;
import org.example.cafe24_demo_v1.order.domain.service.Cafe24OrderPort;
import org.example.cafe24_demo_v1.shared.exception.Cafe24ApiException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.BiFunction;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final int SYNC_PAGE_SIZE = 100;

    private final OrderRepository repository;
    private final Cafe24OrderPort cafe24OrderPort;
    private final AppAuthorizationService authorizationService;
    private final SyncMetricsService syncMetricsService;

    public void syncFromCafe24(String mallId, LocalDateTime updatedSince) {
        TokenCredential credential = authorizationService.getValidCredential(mallId);
        SyncResult result = syncPages(
                mallId,
                (offset, limit) -> cafe24OrderPort.getOrders(mallId, updatedSince, offset, limit, credential)
        );
        syncMetricsService.recordRun(mallId, SyncTarget.ORDER,
                result.processedCount(), result.failedCount(), result.apiFailureCount(), result.errorMessage());

        log.info("Order sync finished: mallId={}, updatedSince={}, processedCount={}, failedCount={}, apiFailureCount={}",
                mallId, updatedSince, result.processedCount(), result.failedCount(), result.apiFailureCount());
    }

    public void backfillFromCafe24(String mallId, LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must be before or equal to endDate");
        }

        TokenCredential credential = authorizationService.getValidCredential(mallId);
        SyncResult result = syncPages(
                mallId,
                (offset, limit) -> cafe24OrderPort.getOrders(mallId, startDate, endDate, offset, limit, credential)
        );
        syncMetricsService.recordRun(mallId, SyncTarget.ORDER,
                result.processedCount(), result.failedCount(), result.apiFailureCount(), result.errorMessage());

        log.info("Order backfill finished: mallId={}, startDate={}, endDate={}, processedCount={}, failedCount={}, apiFailureCount={}",
                mallId, startDate, endDate, result.processedCount(), result.failedCount(), result.apiFailureCount());
    }

    public void upsertFromWebhook(String mallId, String orderId) {
        TokenCredential credential = authorizationService.getValidCredential(mallId);
        Order snapshot = cafe24OrderPort.getOrder(mallId, orderId, credential)
                .orElseThrow(() -> new IllegalStateException("Cafe24 order not found: orderId=" + orderId));
        upsert(snapshot);
    }

    /**
     * 페이지 단위로 Cafe24 주문을 조회해 업서트한다.
     * Cafe24 API 호출(pageFetcher) 자체가 실패하면 더 이상 다음 페이지를 시도하지 않고 이번
     * 실행만 안전하게 종료한다 — 누락된 나머지는 다음 스케줄 실행(lookback 슬라이딩 윈도우)이
     * 자연스럽게 보완하므로, 여기서 예외를 다시 던져 스케줄러까지 전파시키지 않는다.
     */
    private SyncResult syncPages(String mallId, BiFunction<Integer, Integer, List<Order>> pageFetcher) {
        int offset = 0;
        int processedCount = 0;
        int failedCount = 0;
        List<Order> page;
        while (true) {
            try {
                page = pageFetcher.apply(offset, SYNC_PAGE_SIZE);
            } catch (Cafe24ApiException e) {
                log.error("Order sync Cafe24 API 호출 실패, 이번 실행 중단: mallId={}, offset={}", mallId, offset, e);
                return new SyncResult(processedCount, failedCount, 1, e.getMessage());
            }
            for (Order snapshot : page) {
                try {
                    upsert(snapshot);
                    processedCount++;
                } catch (Exception e) {
                    log.error("Order sync item failed, continuing: mallId={}, orderId={}",
                            mallId, snapshot.getOrderId(), e);
                    failedCount++;
                }
            }
            offset += SYNC_PAGE_SIZE;
            if (page.size() < SYNC_PAGE_SIZE) {
                break;
            }
        }

        return new SyncResult(processedCount, failedCount, 0, null);
    }

    private record SyncResult(int processedCount, int failedCount, int apiFailureCount, String errorMessage) {}

    private void upsert(Order snapshot) {
        try {
            findAndApply(snapshot);
        } catch (DataIntegrityViolationException e) {
            log.info("Order insert conflict, retrying as update: mallId={}, orderId={}",
                    snapshot.getMallId(), snapshot.getOrderId());
            repository.findByMallIdAndOrderId(snapshot.getMallId(), snapshot.getOrderId())
                    .ifPresent(existing -> applySnapshotAndSave(existing, snapshot));
        }
    }

    private void findAndApply(Order snapshot) {
        repository.findByMallIdAndOrderId(snapshot.getMallId(), snapshot.getOrderId())
                .ifPresentOrElse(
                        existing -> applySnapshotAndSave(existing, snapshot),
                        () -> repository.save(snapshot)
                );
    }

    private void applySnapshotAndSave(Order existing, Order snapshot) {
        existing.applySnapshot(
                snapshot.getOrderStatus(), snapshot.getMemberId(), snapshot.getBuyerName(), snapshot.getBuyerEmail(),
                snapshot.getTotalAmount(), snapshot.getPaymentMethod(), snapshot.getOrderedAt(), snapshot.getRawJson(),
                snapshot.getEmbeds()
        );
        repository.save(existing);
    }
}
