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

import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문(Order) 관련 유즈케이스를 조율하는 애플리케이션 서비스.
 *
 * Cafe24 API 호출에 필요한 액세스 토큰은 authorization 컨텍스트(AppAuthorizationService)에서
 * 가져온다. order → authorization 단방향 의존이며, product → authorization 의존과 같은 패턴이다.
 *
 * Cafe24 호출(외부 I/O)에는 트랜잭션을 걸지 않는다 — DB 커넥션을 외부 응답 대기 시간만큼
 * 점유하지 않기 위함이다. 실제 DB 반영(upsert)은 주문 한 건 단위로 독립적으로 처리된다.
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
     *
     * 한 건의 upsert가 실패해도 나머지 건은 계속 처리한다 — 동시성 경쟁이나 일시적 오류로 인한
     * 한 건의 실패가 같은 배치의 다른 주문 반영까지 막아서는 안 되기 때문이다.
     */
    public void syncFromCafe24(String mallId, LocalDateTime updatedSince) {
        TokenCredential credential = authorizationService.getValidCredential(mallId);

        int offset = 0;
        int syncedCount = 0;
        List<Order> page;
        do {
            page = cafe24OrderPort.getOrders(mallId, updatedSince, offset, SYNC_PAGE_SIZE, credential);
            for (Order snapshot : page) {
                try {
                    upsert(snapshot);
                    syncedCount++;
                } catch (Exception e) {
                    log.error("Order sync 중 1건 실패, 다음 건 계속 진행: mallId={}, orderId={}",
                            mallId, snapshot.getOrderId(), e);
                }
            }
            offset += SYNC_PAGE_SIZE;
        } while (page.size() == SYNC_PAGE_SIZE);

        log.info("Order sync finished: mallId={}, updatedSince={}, syncedCount={}", mallId, updatedSince, syncedCount);
    }

    /**
     * Webhook으로 주문 생성 알림을 받았을 때 호출한다.
     * Webhook payload에는 일부 필드만 담겨 있을 수 있어, orderId로 Cafe24에서 상세를 다시 조회해 반영한다.
     */
    public void upsertFromWebhook(String mallId, String orderId) {
        TokenCredential credential = authorizationService.getValidCredential(mallId);
        Order snapshot = cafe24OrderPort.getOrder(mallId, orderId, credential)
                .orElseThrow(() -> new IllegalStateException("Cafe24에서 주문을 찾을 수 없음: orderId=" + orderId));
        upsert(snapshot);
    }

    /**
     * Cafe24 스냅샷을 로컬 DB에 반영한다. 이미 있으면 갱신, 없으면 신규 저장(Upsert).
     *
     * (mall_id, order_id) unique 제약 때문에, 같은 주문을 동시에 동기화하는 다른 경로(웹훅 비동기
     * 처리와 스케줄러 동기화가 같은 주문을 동시에 건드리는 경우)와 경쟁하면 INSERT가
     * DataIntegrityViolationException으로 실패할 수 있다. 이 경우 다른 트랜잭션이 먼저 넣은 행을
     * 재조회해 갱신으로 폴백한다.
     */
    private void upsert(Order snapshot) {
        try {
            findAndApply(snapshot);
        } catch (DataIntegrityViolationException e) {
            log.info("동시 삽입 경쟁으로 충돌, 재조회 후 갱신으로 폴백: mallId={}, orderId={}",
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
