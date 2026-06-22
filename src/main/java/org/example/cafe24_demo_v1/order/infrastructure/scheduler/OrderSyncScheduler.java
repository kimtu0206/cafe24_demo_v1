package org.example.cafe24_demo_v1.order.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.order.application.service.OrderService;
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 하루(24시간)에 한 번 Cafe24 주문 중 최근 변경분을 조회해 로컬 DB와 동기화하는 스케줄러.
 * Webhook 수신 여부와 무관하게 독립적으로 동작하는 안전망이다(웹훅 누락 보완용).
 *
 * "마지막 조회 시점"을 별도로 저장하지 않고, 매 실행마다 "현재 - 주기*2"를 하한으로 쓰는
 * 슬라이딩 윈도우 방식이다. 주문 upsert가 멱등적이라 약간의 중복 조회는 무해하고,
 * 서버 재시작 시에도 커서가 유실될 걱정이 없다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSyncScheduler {

    private static final long SYNC_INTERVAL_HOURS = 24;
    private static final long LOOKBACK_MULTIPLIER = 2;

    private final OrderService orderService;
    private final Cafe24Properties cafe24Properties;

    @Scheduled(fixedRate = SYNC_INTERVAL_HOURS * 60 * 60 * 1000L)
    public void syncOrders() {
        String mallId = cafe24Properties.getMallId();
        LocalDateTime updatedSince = LocalDateTime.now().minusHours(SYNC_INTERVAL_HOURS * LOOKBACK_MULTIPLIER);
        log.info("Order sync scheduler triggered: mallId={}, updatedSince={}", mallId, updatedSince);
        orderService.syncFromCafe24(mallId, updatedSince);
    }
}
