package org.example.cafe24_demo_v1.product.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.product.application.service.ProductService;
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 매일 23시, Cafe24 상품 전체를 Local DB와 동기화하는 스케줄러.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductSyncScheduler {

    private final ProductService productService;
    private final Cafe24Properties cafe24Properties;

    @Scheduled(cron = "0 0 10 * * *")
    public void syncProducts() {
        String mallId = cafe24Properties.getMallId();
        log.info("Product sync scheduler triggered: mallId={}", mallId);
        productService.syncFromCafe24(mallId);
    }
}
