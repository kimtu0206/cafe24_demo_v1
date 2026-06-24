package org.example.cafe24_demo_v1.product.domain.repository;

import org.example.cafe24_demo_v1.product.domain.model.Product;
import org.example.cafe24_demo_v1.product.domain.model.ProductPage;

import java.util.List;
import java.util.Optional;

/**
 * Product 저장소 포트(인터페이스). 구현체는 infrastructure 레이어가 담당한다.
 */
public interface ProductRepository {

    Optional<Product> findByMallIdAndProductNo(String mallId, Long productNo);

    /** mallId 기준으로 상품 목록을 페이지 단위로 조회한다. page는 0부터 시작한다. */
    ProductPage findByMallId(String mallId, int page, int size);

    /** mallId 기준으로 저장된 상품 전체를 페이지네이션 없이 조회한다(전체 동기화의 삭제 누락 보정용). */
    List<Product> findAllByMallId(String mallId);

    void save(Product product);

    void deleteByMallIdAndProductNo(String mallId, Long productNo);
}
