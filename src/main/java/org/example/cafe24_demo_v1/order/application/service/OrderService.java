package org.example.cafe24_demo_v1.order.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.authorization.application.service.AppAuthorizationService;
import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.order.domain.model.Order;
import org.example.cafe24_demo_v1.order.domain.repository.OrderRepository;
import org.example.cafe24_demo_v1.order.domain.service.Cafe24OrderPort;
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

    public void syncFromCafe24(String mallId, LocalDateTime updatedSince) {
        TokenCredential credential = authorizationService.getValidCredential(mallId);
        int syncedCount = syncPages(
                mallId,
                (offset, limit) -> cafe24OrderPort.getOrders(mallId, updatedSince, offset, limit, credential)
        );

        log.info("Order sync finished: mallId={}, updatedSince={}, syncedCount={}", mallId, updatedSince, syncedCount);
    }

    public void backfillFromCafe24(String mallId, LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must be before or equal to endDate");
        }

        TokenCredential credential = authorizationService.getValidCredential(mallId);
        int syncedCount = syncPages(
                mallId,
                (offset, limit) -> cafe24OrderPort.getOrders(mallId, startDate, endDate, offset, limit, credential)
        );

        log.info("Order backfill finished: mallId={}, startDate={}, endDate={}, syncedCount={}",
                mallId, startDate, endDate, syncedCount);
    }

    public void upsertFromWebhook(String mallId, String orderId) {
        TokenCredential credential = authorizationService.getValidCredential(mallId);
        Order snapshot = cafe24OrderPort.getOrder(mallId, orderId, credential)
                .orElseThrow(() -> new IllegalStateException("Cafe24 order not found: orderId=" + orderId));
        upsert(snapshot);
    }

    private int syncPages(String mallId, BiFunction<Integer, Integer, List<Order>> pageFetcher) {
        int offset = 0;
        int syncedCount = 0;
        List<Order> page;
        do {
            page = pageFetcher.apply(offset, SYNC_PAGE_SIZE);
            for (Order snapshot : page) {
                try {
                    upsert(snapshot);
                    syncedCount++;
                } catch (Exception e) {
                    log.error("Order sync item failed, continuing: mallId={}, orderId={}",
                            mallId, snapshot.getOrderId(), e);
                }
            }
            offset += SYNC_PAGE_SIZE;
        } while (page.size() == SYNC_PAGE_SIZE);

        return syncedCount;
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
