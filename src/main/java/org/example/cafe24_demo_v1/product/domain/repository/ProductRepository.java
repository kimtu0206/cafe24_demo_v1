package org.example.cafe24_demo_v1.product.domain.repository;

import org.example.cafe24_demo_v1.product.domain.model.Product;

import java.util.List;
import java.util.Optional;

/**
 * Product 저장소 포트(인터페이스). 구현체는 infrastructure 레이어가 담당한다.
 */
public interface ProductRepository {

    Optional<Product> findByMallIdAndProductNo(String mallId, Long productNo);

    List<Product> findAll();

    void save(Product product);

    void deleteByMallIdAndProductNo(String mallId, Long productNo);
}
