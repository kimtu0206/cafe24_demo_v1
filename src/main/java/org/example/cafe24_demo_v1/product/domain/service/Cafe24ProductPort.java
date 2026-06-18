package org.example.cafe24_demo_v1.product.domain.service;

import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.product.domain.model.Product;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cafe24 Admin 상품 API와 통신하는 외부 포트(Port) 인터페이스.
 *
 * 도메인 레이어에 위치하지만 구현체는 infrastructure 레이어의 Cafe24ProductClient가 담당한다.
 * 도메인/애플리케이션 레이어는 이 인터페이스만 알고 실제 HTTP 통신 방식은 모른다.
 */
public interface Cafe24ProductPort {

    /** 신규 상품을 Cafe24에 등록하고, 등록 결과를 도메인 모델로 반환한다. */
    Product createProduct(
            String mallId,
            String productName,
            BigDecimal price,
            BigDecimal supplyPrice,
            TokenCredential credential
    );

    /** 상품 번호로 Cafe24에 등록된 상품 상세 정보를 조회한다. */
    Product getProduct(String mallId, Long productNo, TokenCredential credential);

    /** offset/limit 페이지네이션으로 Cafe24 상품 목록 한 페이지를 조회한다. */
    List<Product> getProducts(String mallId, int offset, int limit, TokenCredential credential);

    /** 기존 상품을 Cafe24에서 수정하고, 수정 결과를 도메인 모델로 반환한다. */
    Product updateProduct(
            String mallId,
            Long productNo,
            String productName,
            BigDecimal price,
            BigDecimal supplyPrice,
            TokenCredential credential
    );

    /** 상품을 Cafe24에서 삭제한다. */
    void deleteProduct(String mallId, Long productNo, TokenCredential credential);
}
