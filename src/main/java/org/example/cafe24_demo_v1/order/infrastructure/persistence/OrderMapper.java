package org.example.cafe24_demo_v1.order.infrastructure.persistence;

import org.example.cafe24_demo_v1.order.domain.model.Order;
import org.springframework.stereotype.Component;

/**
 * 도메인 모델(Order) ↔ JPA 엔티티(OrderEntity) 간 변환을 담당하는 매퍼.
 */
@Component
class OrderMapper {

    /** JPA 엔티티 → 도메인 모델 변환 (DB에서 조회 후 도메인으로 복원할 때 사용) */
    Order toDomain(OrderEntity entity) {
        return Order.reconstitute(
                entity.getId(),
                entity.getMallId(),
                entity.getOrderId(),
                entity.getOrderStatus(),
                entity.getMemberId(),
                entity.getBuyerName(),
                entity.getBuyerEmail(),
                entity.getTotalAmount(),
                entity.getPaymentMethod(),
                entity.getOrderedAt(),
                entity.getRawJson(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /** 도메인 모델 → JPA 엔티티 변환 (저장할 때 사용) */
    OrderEntity toEntity(Order domain) {
        OrderEntity entity = new OrderEntity();
        entity.setId(domain.getId());
        entity.setMallId(domain.getMallId());
        entity.setOrderId(domain.getOrderId());
        entity.setOrderStatus(domain.getOrderStatus());
        entity.setMemberId(domain.getMemberId());
        entity.setBuyerName(domain.getBuyerName());
        entity.setBuyerEmail(domain.getBuyerEmail());
        entity.setTotalAmount(domain.getTotalAmount());
        entity.setPaymentMethod(domain.getPaymentMethod());
        entity.setOrderedAt(domain.getOrderedAt());
        entity.setRawJson(domain.getRawJson());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
