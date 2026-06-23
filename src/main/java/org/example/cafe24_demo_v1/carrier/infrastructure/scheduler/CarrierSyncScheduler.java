package org.example.cafe24_demo_v1.carrier.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.carrier.application.service.CarrierService;
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 매일 10시, Cafe24 배송사 전체를 Local DB와 동기화하는 스케줄러.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CarrierSyncScheduler {

    private final CarrierService carrierService;
    private final Cafe24Properties cafe24Properties;

    @Scheduled(cron = "0 30 16 * * *")
    public void syncCarriers() {
        String mallId = cafe24Properties.getMallId();
        log.info("Carrier sync scheduler triggered: mallId={}", mallId);
        carrierService.syncFromCafe24(mallId);
    }
}
