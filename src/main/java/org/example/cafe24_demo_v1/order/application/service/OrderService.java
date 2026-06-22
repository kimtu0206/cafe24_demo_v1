package org.example.cafe24_demo_v1.order.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.authorization.application.service.AppAuthorizationService;
import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.order.domain.model.Order;
import org.example.cafe24_demo_v1.order.domain.repository.OrderRepository;
import org.example.cafe24_demo_v1.order.domain.service.Cafe24OrderPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문(Order) 관련 유즈케이스를 조율하는 애플리케이션 서비스.
 *
 * Cafe24 API 호출에 필요한 액세스 토큰은 authorization 컨텍스트(AppAuthorizationService)에서
 * 가져온다. order → authorization 단방향 의존이며, product → authorization 의존과 같은 패턴이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final int SYNC_PAGE_SIZE = 100;

    private final OrderRepository repository;
    private final Cafe24OrderPort cafe24OrderPort;
    private final AppAuthorizationService authorizationService;

    /**
     * updatedSince 이후 변경된 주문을 Cafe24에서 페이지 단위로 조회해 로컬 DB와 동기화한다.
     * Webhook 수신 여부와 무관하게 독립적으로 동작하는 안전망 역할이다(OrderSyncScheduler가 주기 호출).
     */
    @Transactional
    public void syncFromCafe24(String mallId, LocalDateTime updatedSince) {
        TokenCredential credential = authorizationService.getValidCredential(mallId);

        int offset = 0;
        int syncedCount = 0;
        List<Order> page;
        do {
            page = cafe24OrderPort.getOrders(mallId, updatedSince, offset, SYNC_PAGE_SIZE, credential);
            page.forEach(this::upsert);
            syncedCount += page.size();
            offset += SYNC_PAGE_SIZE;
        } while (page.size() == SYNC_PAGE_SIZE);

        log.info("Order sync finished: mallId={}, updatedSince={}, syncedCount={}", mallId, updatedSince, syncedCount);
    }

    /**
     * Webhook으로 주문 생성 알림을 받았을 때 호출한다.
     * Webhook payload에는 일부 필드만 담겨 있을 수 있어, orderId로 Cafe24에서 상세를 다시 조회해 반영한다.
     */
    @Transactional
    public void upsertFromWebhook(String mallId, String orderId) {
        TokenCredential credential = authorizationService.getValidCredential(mallId);
        Order snapshot = cafe24OrderPort.getOrder(mallId, orderId, credential)
                .orElseThrow(() -> new IllegalStateException("Cafe24에서 주문을 찾을 수 없음: orderId=" + orderId));
        upsert(snapshot);
    }

    /** Cafe24 스냅샷을 로컬 DB에 반영한다. 이미 있으면 갱신, 없으면 신규 저장(Upsert). */
    private void upsert(Order snapshot) {
        repository.findByMallIdAndOrderId(snapshot.getMallId(), snapshot.getOrderId())
                .ifPresentOrElse(
                        existing -> {
                            existing.applySnapshot(
                                    snapshot.getOrderStatus(), snapshot.getMemberId(), snapshot.getBuyerName(), snapshot.getBuyerEmail(),
                                    snapshot.getTotalAmount(), snapshot.getPaymentMethod(), snapshot.getOrderedAt(), snapshot.getRawJson(),
                                    snapshot.getEmbeds()
                            );
                            repository.save(existing);
                        },
                        () -> repository.save(snapshot)
                );
    }
}
