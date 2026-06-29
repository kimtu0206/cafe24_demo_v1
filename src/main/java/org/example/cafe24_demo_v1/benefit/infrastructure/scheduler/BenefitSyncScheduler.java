package org.example.cafe24_demo_v1.benefit.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.benefit.application.service.BenefitService;
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BenefitSyncScheduler {

    private final BenefitService benefitService;
    private final Cafe24Properties cafe24Properties;

    @Scheduled(cron = "${sync.benefit.cron}")
    public void syncBenefits() {
        String mallId = cafe24Properties.getMallId();
        log.info("Benefit sync scheduler triggered: mallId={}", mallId);
        benefitService.syncFromCafe24(mallId);
    }
}
