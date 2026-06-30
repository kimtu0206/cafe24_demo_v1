package org.example.cafe24_demo_v1.product.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.authorization.application.service.AppAuthorizationService;
import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.monitoring.application.service.SyncMetricsService;
import org.example.cafe24_demo_v1.monitoring.domain.model.SyncTarget;
import org.example.cafe24_demo_v1.product.application.command.CreateProductCommand;
import org.example.cafe24_demo_v1.product.application.command.DeleteProductCommand;
import org.example.cafe24_demo_v1.product.application.command.UpdateProductCommand;
import org.example.cafe24_demo_v1.product.domain.model.Product;
import org.example.cafe24_demo_v1.product.domain.model.ProductPage;
import org.example.cafe24_demo_v1.product.domain.model.ProductRegistration;
import org.example.cafe24_demo_v1.product.domain.repository.ProductRepository;
import org.example.cafe24_demo_v1.product.domain.service.Cafe24ProductPort;
import org.example.cafe24_demo_v1.shared.application.SyncResult;
import org.example.cafe24_demo_v1.shared.exception.Cafe24ApiException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 상품(Product) 관련 유즈케이스를 조율하는 애플리케이션 서비스.
 *
 * Cafe24 API 호출에 필요한 액세스 토큰은 authorization 컨텍스트(AppAuthorizationService)에서
 * 가져온다. product → authorization 단방향 의존이며, webhook → authorization 의존과 같은 패턴이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private static final int SYNC_PAGE_SIZE = 100;
    private static final int MAX_PAGE_SIZE = 100;

    private final ProductRepository repository;
    private final Cafe24ProductPort cafe24ProductPort;
    private final AppAuthorizationService authorizationService;
    private final SyncMetricsService syncMetricsService;

    /**
     * Cafe24에 신규 상품을 등록하고, 등록 결과(product_no, 상품명, 판매가, 상태)를 로컬 DB에 저장한다.
     * Cafe24 호출(외부 I/O)에는 트랜잭션을 걸지 않는다 — DB 커넥션을 외부 응답 대기 시간만큼 점유하지
     * 않기 위함이다(syncFromCafe24와 동일한 이유).
     */
    public Product register(CreateProductCommand command) {
        TokenCredential credential = authorizationService.getValidCredential(command.mallId());

        ProductRegistration registration = new ProductRegistration(
                command.productName(), command.price(), command.supplyPrice(),
                command.description(), command.paymentInfo(), command.shippingInfo(), command.exchangeInfo(),
                command.priceExcludingTax(), command.detailImage(), command.imageUploadType()
        );
        Product product = cafe24ProductPort.createProduct(command.mallId(), registration, credential);

        repository.save(product);
        return product;
    }

    /** mallId 기준으로 로컬 DB에 저장된 상품 목록을 페이지 단위로 조회한다(Cafe24를 호출하지 않음). */
    public ProductPage list(String mallId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return repository.findByMallId(mallId, safePage, safeSize);
    }

    /**
     * Cafe24에서 기존 상품을 수정하고, 수정 결과를 로컬 DB에 반영한다(Upsert).
     * Cafe24 호출(외부 I/O)에는 트랜잭션을 걸지 않는다 — DB 커넥션을 외부 응답 대기 시간만큼 점유하지
     * 않기 위함이다(syncFromCafe24와 동일한 이유).
     */
    public Product update(UpdateProductCommand command) {
        TokenCredential credential = authorizationService.getValidCredential(command.mallId());

        ProductRegistration registration = new ProductRegistration(
                command.productName(), command.price(), command.supplyPrice(),
                command.description(), command.paymentInfo(), command.shippingInfo(), command.exchangeInfo(),
                command.priceExcludingTax(), command.detailImage(), command.imageUploadType()
        );
        Product updated = cafe24ProductPort.updateProduct(command.mallId(), command.productNo(), registration, credential);

        upsert(updated);
        return updated;
    }

    /**
     * Cafe24에서 상품을 삭제하고, 로컬 DB에서도 동일 상품을 삭제한다.
     * Cafe24 호출(외부 I/O)에는 트랜잭션을 걸지 않는다 — DB 커넥션을 외부 응답 대기 시간만큼 점유하지
     * 않기 위함이다(syncFromCafe24와 동일한 이유).
     */
    public void delete(DeleteProductCommand command) {
        TokenCredential credential = authorizationService.getValidCredential(command.mallId());

        cafe24ProductPort.deleteProduct(command.mallId(), command.productNo(), credential);
        repository.deleteByMallIdAndProductNo(command.mallId(), command.productNo());
    }

    /**
     * Webhook으로 상품 생성/수정 알림을 받았을 때 호출한다.
     * Cafe24 Webhook 알림에는 product_no만 담겨 있으므로 상세 정보를 다시 조회해 로컬 DB에 반영한다.
     * Cafe24 호출(외부 I/O)에는 트랜잭션을 걸지 않는다 — DB 커넥션을 외부 응답 대기 시간만큼 점유하지
     * 않기 위함이다(syncFromCafe24와 동일한 이유).
     */
    public void upsertFromWebhook(String mallId, Long productNo) {
        TokenCredential credential = authorizationService.getValidCredential(mallId);
        Product snapshot = cafe24ProductPort.getProduct(mallId, productNo, credential);
        upsert(snapshot);
    }

    /**
     * Webhook으로 상품 삭제 알림을 받았을 때 호출한다.
     * Cafe24에는 이미 삭제된 상태이므로 API를 다시 호출하지 않고 로컬 DB에서만 제거한다.
     */
    @Transactional
    public void deleteFromWebhook(String mallId, Long productNo) {
        repository.deleteByMallIdAndProductNo(mallId, productNo);
    }

    /**
     * Cafe24 상품 전체를 페이지 단위로 조회해 로컬 DB와 동기화한다.
     * 신규/변경 상품은 upsert하고, Cafe24 응답에 없는(로컬에만 남은) 상품은
     * reconcileMissingProducts로 단계적으로 정리한다. 매일 23시 ProductSyncScheduler가 호출한다.
     *
     * Cafe24 호출(외부 I/O)에는 트랜잭션을 걸지 않는다 — DB 커넥션을 외부 응답 대기 시간만큼
     * 점유하지 않기 위함이다(Order와 동일한 이유). 한 건의 upsert가 실패해도 나머지 건은 계속
     * 처리한다 — 한 건의 실패가 같은 배치의 다른 상품 반영까지 막아서는 안 되기 때문이다.
     */
    public SyncResult syncFromCafe24(String mallId) {
        TokenCredential credential;
        try {
            credential = authorizationService.getValidCredential(mallId);
        } catch (Exception e) {
            log.error("Product sync 인증 실패, 이번 실행 중단: mallId={}", mallId, e);
            SyncResult failure = new SyncResult(0, 0, 1, e.getMessage());
            syncMetricsService.recordRun(mallId, SyncTarget.PRODUCT,
                    failure.processedCount(), failure.failedCount(), failure.apiFailureCount(), failure.errorMessage());
            return failure;
        }

        PageSyncResult pageResult = syncPages(mallId, credential);
        if (pageResult.apiFailureCount() == 0) {
            // Cafe24 API 호출이 끝까지 성공했을 때만 전체 목록(seenProductNos)이 완전하므로,
            // 중간에 실패해 일부만 조회된 상태로 reconcileMissingProducts를 돌리면 정상 상품을
            // 누락으로 잘못 판단할 수 있다.
            reconcileMissingProducts(mallId, pageResult.seenProductNos());
        }
        SyncResult result = new SyncResult(
                pageResult.processedCount(), pageResult.failedCount(),
                pageResult.apiFailureCount(), pageResult.errorMessage());
        syncMetricsService.recordRun(mallId, SyncTarget.PRODUCT,
                result.processedCount(), result.failedCount(), result.apiFailureCount(), result.errorMessage());

        log.info("Product sync finished: mallId={}, processedCount={}, failedCount={}, apiFailureCount={}",
                mallId, result.processedCount(), result.failedCount(), result.apiFailureCount());
        return result;
    }

    /**
     * Cafe24 API 호출(getProducts) 자체가 실패하면 더 이상 다음 페이지를 시도하지 않고 이번
     * 실행만 안전하게 종료한다 — 누락된 나머지는 다음 스케줄 실행이 보완한다.
     */
    private PageSyncResult syncPages(String mallId, TokenCredential credential) {
        int offset = 0;
        int processedCount = 0;
        int failedCount = 0;
        Set<Long> seenProductNos = new HashSet<>();
        List<Product> page;
        while (true) {
            try {
                page = cafe24ProductPort.getProducts(mallId, offset, SYNC_PAGE_SIZE, credential);
            } catch (Cafe24ApiException e) {
                log.error("Product sync Cafe24 API 호출 실패, 이번 실행 중단: mallId={}, offset={}", mallId, offset, e);
                return new PageSyncResult(processedCount, failedCount, 1, e.getMessage(), seenProductNos);
            }
            for (Product snapshot : page) {
                seenProductNos.add(snapshot.getProductNo());
                try {
                    upsert(snapshot);
                    processedCount++;
                } catch (Exception e) {
                    log.error("Product sync 중 1건 실패, 다음 건 계속 진행: mallId={}, productNo={}",
                            mallId, snapshot.getProductNo(), e);
                    failedCount++;
                }
            }
            offset += SYNC_PAGE_SIZE;
            if (page.size() < SYNC_PAGE_SIZE) {
                break;
            }
        }

        return new PageSyncResult(processedCount, failedCount, 0, null, seenProductNos);
    }

    private record PageSyncResult(int processedCount, int failedCount, int apiFailureCount, String errorMessage, Set<Long> seenProductNos) {}

    /**
     * 이번 전체 동기화에서 Cafe24 응답에 없었던 로컬 상품을 정리한다.
     * 1차 누락(missingSince가 null)이면 STALE 표시만 하고, 이미 STALE인데 또 누락되면(2회 연속)
     * Cafe24에서 실제로 삭제된 것으로 보고 로컬에서도 삭제한다. 다시 나타나면 upsert 경로의
     * applySnapshot이 missingSince를 초기화하므로 별도 복구 처리는 필요 없다.
     */
    private void reconcileMissingProducts(String mallId, Set<Long> seenProductNos) {
        for (Product local : repository.findAllByMallId(mallId)) {
            if (seenProductNos.contains(local.getProductNo())) {
                continue;
            }
            try {
                if (local.getMissingSince() == null) {
                    local.markMissing();
                    repository.save(local);
                    log.info("Product 1차 누락 감지, STALE 처리: mallId={}, productNo={}", mallId, local.getProductNo());
                } else {
                    repository.deleteByMallIdAndProductNo(mallId, local.getProductNo());
                    log.info("Product 2회 연속 누락 확인, 로컬 삭제: mallId={}, productNo={}", mallId, local.getProductNo());
                }
            } catch (Exception e) {
                log.error("Product 누락 보정 중 1건 실패, 다음 건 계속 진행: mallId={}, productNo={}",
                        mallId, local.getProductNo(), e);
            }
        }
    }

    /**
     * Cafe24 스냅샷을 로컬 DB에 반영한다. 이미 있으면 갱신, 없으면 신규 저장(Upsert).
     *
     * (mall_id, product_no) unique 제약 때문에, 같은 상품을 동시에 반영하는 다른 경로
     * (syncFromCafe24/update/Webhook 등)와 경쟁하면 INSERT가 DataIntegrityViolationException으로
     * 실패할 수 있다. 이 경우 다른 트랜잭션이 먼저 넣은 행을 재조회해 갱신으로 폴백한다(Order.upsert와
     * 동일한 패턴). 이 메서드의 모든 호출부가 @Transactional 없이 호출되어 save()마다 독립된
     * 트랜잭션으로 즉시 flush되므로 이 폴백이 안전하게 동작한다.
     */
    private void upsert(Product snapshot) {
        try {
            findAndApply(snapshot);
        } catch (DataIntegrityViolationException e) {
            log.info("Product 동시 삽입 경쟁으로 충돌, 재조회 후 갱신으로 폴백: mallId={}, productNo={}",
                    snapshot.getMallId(), snapshot.getProductNo());
            repository.findByMallIdAndProductNo(snapshot.getMallId(), snapshot.getProductNo())
                    .ifPresent(existing -> applySnapshotAndSave(existing, snapshot));
        }
    }

    private void findAndApply(Product snapshot) {
        repository.findByMallIdAndProductNo(snapshot.getMallId(), snapshot.getProductNo())
                .ifPresentOrElse(
                        existing -> applySnapshotAndSave(existing, snapshot),
                        () -> repository.save(snapshot)
                );
    }

    private void applySnapshotAndSave(Product existing, Product snapshot) {
        existing.applySnapshot(
                snapshot.getProductName(), snapshot.getPrice(), snapshot.getSupplyPrice(), snapshot.getStatus(),
                snapshot.getDescription(), snapshot.getPaymentInfo(), snapshot.getShippingInfo(), snapshot.getExchangeInfo(),
                snapshot.getPriceExcludingTax(), snapshot.getDetailImage(), snapshot.getImageUploadType()
        );
        repository.save(existing);
    }
}
