package org.example.cafe24_demo_v1.carrier.infrastructure.persistence;

import org.example.cafe24_demo_v1.carrier.domain.model.Carrier;
import org.example.cafe24_demo_v1.carrier.domain.model.ShippingType;
import org.springframework.stereotype.Component;

/**
 * 도메인 모델(Carrier) ↔ JPA 엔티티(CarrierEntity) 간 변환을 담당하는 매퍼.
 */
@Component
class CarrierMapper {

    /** JPA 엔티티 → 도메인 모델 변환 (DB에서 조회 후 도메인으로 복원할 때 사용) */
    Carrier toDomain(CarrierEntity entity) {
        return Carrier.reconstitute(
                entity.getId(),
                entity.getCarrierId(),
                entity.getMallId(),
                entity.getShippingCarrierCode(),
                entity.getShippingCarrierName(),
                entity.getContact(),
                entity.getSecondaryContact(),
                entity.getEmail(),
                entity.getTrackShipmentUrl(),
                entity.getDefaultShippingFee(),
                entity.getHomepageUrl(),
                ShippingType.valueOf(entity.getShippingType()),
                entity.isDefaultCarrier(),
                entity.isShippingFeeSetting(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /** 도메인 모델 → JPA 엔티티 변환 (저장할 때 사용) */
    CarrierEntity toEntity(Carrier domain) {
        CarrierEntity entity = new CarrierEntity();
        entity.setId(domain.getId());
        entity.setCarrierId(domain.getCarrierId());
        entity.setMallId(domain.getMallId());
        entity.setShippingCarrierCode(domain.getShippingCarrierCode());
        entity.setShippingCarrierName(domain.getShippingCarrierName());
        entity.setContact(domain.getContact());
        entity.setSecondaryContact(domain.getSecondaryContact());
        entity.setEmail(domain.getEmail());
        entity.setTrackShipmentUrl(domain.getTrackShipmentUrl());
        entity.setDefaultShippingFee(domain.getDefaultShippingFee());
        entity.setHomepageUrl(domain.getHomepageUrl());
        entity.setShippingType(domain.getShippingType().name());
        entity.setDefaultCarrier(domain.isDefaultCarrier());
        entity.setShippingFeeSetting(domain.isShippingFeeSetting());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
