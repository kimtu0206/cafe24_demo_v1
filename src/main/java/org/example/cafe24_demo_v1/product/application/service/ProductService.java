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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
     * 신규/변경 상품만 반영하고, 로컬에만 있고 Cafe24에는 없는 상품은 그대로 둔다.
     * 매일 23시 ProductSyncScheduler가 호출한다.
     */
    @Transactional
    public void syncFromCafe24(String mallId) {
        TokenCredential credential = authorizationService.getValidCredential(mallId);

        int offset = 0;
        int syncedCount = 0;
        List<Product> page;
        do {
            page = cafe24ProductPort.getProducts(mallId, offset, SYNC_PAGE_SIZE, credential);
            page.forEach(this::upsert);
            syncedCount += page.size();
            offset += SYNC_PAGE_SIZE;
        } while (page.size() == SYNC_PAGE_SIZE);

        log.info("Product sync finished: mallId={}, syncedCount={}", mallId, syncedCount);
    }

    /** Cafe24 스냅샷을 로컬 DB에 반영한다. 이미 있으면 갱신, 없으면 신규 저장(Upsert). */
    private void upsert(Product snapshot) {
        repository.findByMallIdAndProductNo(snapshot.getMallId(), snapshot.getProductNo())
                .ifPresentOrElse(
                        existing -> {
                            existing.applySnapshot(
                                    snapshot.getProductName(), snapshot.getPrice(), snapshot.getSupplyPrice(), snapshot.getStatus(),
                                    snapshot.getDescription(), snapshot.getPaymentInfo(), snapshot.getShippingInfo(), snapshot.getExchangeInfo(),
                                    snapshot.getPriceExcludingTax(), snapshot.getDetailImage(), snapshot.getImageUploadType()
                            );
                            repository.save(existing);
                        },
                        () -> repository.save(snapshot)
                );
    }
}
