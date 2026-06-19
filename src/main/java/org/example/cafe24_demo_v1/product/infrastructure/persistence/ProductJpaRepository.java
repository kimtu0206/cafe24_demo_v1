package org.example.cafe24_demo_v1.product.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Product JPA 레포지토리.
 */
interface ProductJpaRepository extends JpaRepository<ProductEntity, Long> {

    Optional<ProductEntity> findByMallIdAndProductNo(String mallId, Long productNo);

    void deleteByMallIdAndProductNo(String mallId, Long productNo);
}
