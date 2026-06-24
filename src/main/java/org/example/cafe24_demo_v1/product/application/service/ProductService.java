package org.example.cafe24_demo_v1.product.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.authorization.application.service.AppAuthorizationService;
import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.product.application.command.CreateProductCommand;
import org.example.cafe24_demo_v1.product.application.command.DeleteProductCommand;
import org.example.cafe24_demo_v1.product.application.command.UpdateProductCommand;
import org.example.cafe24_demo_v1.product.domain.model.Product;
import org.example.cafe24_demo_v1.product.domain.model.ProductPage;
import org.example.cafe24_demo_v1.product.domain.model.ProductRegistration;
import org.example.cafe24_demo_v1.product.domain.repository.ProductRepository;
import org.example.cafe24_demo_v1.product.domain.service.Cafe24ProductPort;
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

    /** Cafe24에 신규 상품을 등록하고, 등록 결과(product_no, 상품명, 판매가, 상태)를 로컬 DB에 저장한다. */
    @Transactional
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

    /** Cafe24에서 기존 상품을 수정하고, 수정 결과를 로컬 DB에 반영한다(Upsert). */
    @Transactional
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

    /** Cafe24에서 상품을 삭제하고, 로컬 DB에서도 동일 상품을 삭제한다. */
    @Transactional
    public void delete(DeleteProductCommand command) {
        TokenCredential credential = authorizationService.getValidCredential(command.mallId());

        cafe24ProductPort.deleteProduct(command.mallId(), command.productNo(), credential);
        repository.deleteByMallIdAndProductNo(command.mallId(), command.productNo());
    }

    /**
     * Webhook으로 상품 생성/수정 알림을 받았을 때 호출한다.
     * Cafe24 Webhook 알림에는 product_no만 담겨 있으므로 상세 정보를 다시 조회해 로컬 DB에 반영한다.
     */
    @Transactional
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
    public void syncFromCafe24(String mallId) {
        TokenCredential credential = authorizationService.getValidCredential(mallId);

        int offset = 0;
        int syncedCount = 0;
        Set<Long> seenProductNos = new HashSet<>();
        List<Product> page;
        do {
            page = cafe24ProductPort.getProducts(mallId, offset, SYNC_PAGE_SIZE, credential);
            for (Product snapshot : page) {
                seenProductNos.add(snapshot.getProductNo());
                try {
                    upsertWithConflictFallback(snapshot);
                    syncedCount++;
                } catch (Exception e) {
                    log.error("Product sync 중 1건 실패, 다음 건 계속 진행: mallId={}, productNo={}",
                            mallId, snapshot.getProductNo(), e);
                }
            }
            offset += SYNC_PAGE_SIZE;
        } while (page.size() == SYNC_PAGE_SIZE);

        reconcileMissingProducts(mallId, seenProductNos);

        log.info("Product sync finished: mallId={}, syncedCount={}", mallId, syncedCount);
    }

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

    /** Cafe24 스냅샷을 로컬 DB에 반영한다. 이미 있으면 갱신, 없으면 신규 저장(Upsert). */
    private void upsert(Product snapshot) {
        findAndApply(snapshot);
    }

    /**
     * syncFromCafe24 전용 upsert. (mall_id, product_no) unique 제약 때문에, 같은 상품을 동시에
     * 반영하는 다른 경로(Webhook 등)와 경쟁하면 INSERT가 DataIntegrityViolationException으로 실패할
     * 수 있다. 이 경우 다른 트랜잭션이 먼저 넣은 행을 재조회해 갱신으로 폴백한다(Order.upsert와 동일한
     * 패턴). syncFromCafe24는 @Transactional이 없어 save() 호출마다 독립된 트랜잭션으로 즉시
     * flush되므로 이 폴백이 안전하게 동작한다. update()/upsertFromWebhook()은 @Transactional로
     * 감싸여 있어 같은 폴백을 적용하면 트랜잭션이 rollback-only로 마킹돼 폴백 자체가 무효화될 수
     * 있으므로 적용하지 않는다.
     */
    private void upsertWithConflictFallback(Product snapshot) {
        try {
            findAndApply(snapshot);
        } catch (DataIntegrityViolationException e) {
            log.info("Product sync 중 동시 삽입 경쟁으로 충돌, 재조회 후 갱신으로 폴백: mallId={}, productNo={}",
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
