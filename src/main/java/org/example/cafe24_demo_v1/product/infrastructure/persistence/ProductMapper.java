package org.example.cafe24_demo_v1.product.infrastructure.persistence;

import org.example.cafe24_demo_v1.product.domain.model.Product;
import org.example.cafe24_demo_v1.product.domain.model.ProductStatus;
import org.springframework.stereotype.Component;

/**
 * 도메인 모델(Product) ↔ JPA 엔티티(ProductEntity) 간 변환을 담당하는 매퍼.
 */
@Component
class ProductMapper {

    /** JPA 엔티티 → 도메인 모델 변환 (DB에서 조회 후 도메인으로 복원할 때 사용) */
    Product toDomain(ProductEntity entity) {
        return Product.reconstitute(
                entity.getId(),
                entity.getProductNo(),
                entity.getMallId(),
                entity.getProductName(),
                entity.getPrice(),
                entity.getSupplyPrice(),
                ProductStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /** 도메인 모델 → JPA 엔티티 변환 (저장할 때 사용) */
    ProductEntity toEntity(Product domain) {
        ProductEntity entity = new ProductEntity();
        entity.setId(domain.getId());
        entity.setProductNo(domain.getProductNo());
        entity.setMallId(domain.getMallId());
        entity.setProductName(domain.getProductName());
        entity.setPrice(domain.getPrice());
        entity.setSupplyPrice(domain.getSupplyPrice());
        entity.setStatus(domain.getStatus().name());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
