package org.example.cafe24_demo_v1.order.infrastructure.persistence;

import org.example.cafe24_demo_v1.order.domain.model.Order;
import org.example.cafe24_demo_v1.order.domain.model.OrderEmbeddedResources;
import org.example.cafe24_demo_v1.order.domain.model.OrderType;
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
                parseOrderType(entity),
                entity.getMemberId(),
                entity.getBuyerName(),
                entity.getBuyerEmail(),
                entity.getTotalAmount(),
                entity.getPaymentMethod(),
                entity.getOrderedAt(),
                entity.getRawJson(),
                entity.getCanceled(),
                entity.getCancelDate(),
                new OrderEmbeddedResources(
                        entity.getItems(),
                        entity.getReceivers(),
                        entity.getBuyer(),
                        entity.getReturnInfo(),
                        entity.getCancellation(),
                        entity.getExchange()
                ),
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
        entity.setOrderType(domain.getOrderType() != null ? domain.getOrderType().name() : null);
        entity.setMemberId(domain.getMemberId());
        entity.setBuyerName(domain.getBuyerName());
        entity.setBuyerEmail(domain.getBuyerEmail());
        entity.setTotalAmount(domain.getTotalAmount());
        entity.setPaymentMethod(domain.getPaymentMethod());
        entity.setOrderedAt(domain.getOrderedAt());
        entity.setRawJson(domain.getRawJson());
        entity.setCanceled(domain.getCanceled());
        entity.setCancelDate(domain.getCancelDate());
        entity.setItems(domain.getItems());
        entity.setReceivers(domain.getReceivers());
        entity.setBuyer(domain.getBuyer());
        entity.setReturnInfo(domain.getReturnInfo());
        entity.setCancellation(domain.getCancellation());
        entity.setExchange(domain.getExchange());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    /**
     * DB의 order_type 컬럼 값을 OrderType으로 변환한다.
     * 컬럼이 NULL인 레거시 행은 member_id 유무로 추론해 폴백 처리한다.
     */
    private OrderType parseOrderType(OrderEntity entity) {
        if (entity.getOrderType() != null) {
            return OrderType.valueOf(entity.getOrderType());
        }
        String memberId = entity.getMemberId();
        return (memberId != null && !memberId.isBlank()) ? OrderType.MEMBER : OrderType.GUEST;
    }
}
