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
import org.example.cafe24_demo_v1.shared.application.SyncResult;
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

    public SyncResult syncFromCafe24(String mallId, LocalDateTime updatedSince) {
        TokenCredential credential;
        try {
            credential = authorizationService.getValidCredential(mallId);
        } catch (Exception e) {
            return recordAuthFailure(mallId, "Order sync", e);
        }

        SyncResult result = syncPages(
                mallId,
                (offset, limit) -> cafe24OrderPort.getOrders(mallId, updatedSince, offset, limit, credential)
        );
        syncMetricsService.recordRun(mallId, SyncTarget.ORDER,
                result.processedCount(), result.failedCount(), result.apiFailureCount(), result.errorMessage());

        log.info("Order sync finished: mallId={}, updatedSince={}, processedCount={}, failedCount={}, apiFailureCount={}",
                mallId, updatedSince, result.processedCount(), result.failedCount(), result.apiFailureCount());
        return result;
    }

    /**
     * 운영자가 임의 시점에 직접 트리거하는 단발성 재동기화다. 정기 안전망(syncFromCafe24)과는
     * 별개의 운영 작업이므로 SyncMetricsService(정기 동기화 상태 추적용)에는 기록하지 않는다 —
     * 결과는 호출자(AdminBackfillController)에게 SyncResult로 직접 반환되어 그 자리에서 바로
     * 200/502로 확인할 수 있다. 정기 실행의 lastRunAt/lastSuccessAt을 백필 결과로 덮어쓰면
     * "정기 안전망이 잘 도는지"와 "임의 기간 백필 결과"가 섞여 운영 관측을 혼동시키기 때문이다.
     */
    public SyncResult backfillFromCafe24(String mallId, LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must be before or equal to endDate");
        }

        TokenCredential credential;
        try {
            credential = authorizationService.getValidCredential(mallId);
        } catch (Exception e) {
            log.error("Order backfill 인증 실패, 이번 실행 중단: mallId={}", mallId, e);
            return new SyncResult(0, 0, 1, e.getMessage());
        }

        SyncResult result = syncPages(
                mallId,
                (offset, limit) -> cafe24OrderPort.getOrders(mallId, startDate, endDate, offset, limit, credential)
        );

        log.info("Order backfill finished: mallId={}, startDate={}, endDate={}, processedCount={}, failedCount={}, apiFailureCount={}",
                mallId, startDate, endDate, result.processedCount(), result.failedCount(), result.apiFailureCount());
        return result;
    }

    /**
     * 토큰 조회/갱신(getValidCredential) 실패는 Cafe24 API 호출 자체가 실패한 것과 동일하게
     * 취급한다 — 이번 실행을 안전하게 중단하고 SyncMetricsService에 기록한 뒤, 예외를 호출자
     * (스케줄러)까지 전파하지 않는다.
     */
    private SyncResult recordAuthFailure(String mallId, String logPrefix, Exception e) {
        log.error("{} 인증 실패, 이번 실행 중단: mallId={}", logPrefix, mallId, e);
        SyncResult result = new SyncResult(0, 0, 1, e.getMessage());
        syncMetricsService.recordRun(mallId, SyncTarget.ORDER,
                result.processedCount(), result.failedCount(), result.apiFailureCount(), result.errorMessage());
        return result;
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
