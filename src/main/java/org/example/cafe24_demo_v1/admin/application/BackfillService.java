package org.example.cafe24_demo_v1.admin.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.carrier.application.service.CarrierService;
import org.example.cafe24_demo_v1.order.application.service.OrderService;
import org.example.cafe24_demo_v1.product.application.service.ProductService;
import org.example.cafe24_demo_v1.shared.application.SyncResult;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackfillService {

    private final OrderService orderService;
    private final ProductService productService;
    private final CarrierService carrierService;

    public SyncResult backfillOrders(String mallId, LocalDate startDate, LocalDate endDate) {
        log.info("Order backfill requested: mallId={}, startDate={}, endDate={}", mallId, startDate, endDate);
        return orderService.backfillFromCafe24(mallId, startDate, endDate);
    }

    public SyncResult backfillProducts(String mallId) {
        log.info("Product backfill requested: mallId={}", mallId);
        return productService.syncFromCafe24(mallId);
    }

    public SyncResult backfillCarriers(String mallId) {
        log.info("Carrier backfill requested: mallId={}", mallId);
        return carrierService.syncFromCafe24(mallId);
    }
}
