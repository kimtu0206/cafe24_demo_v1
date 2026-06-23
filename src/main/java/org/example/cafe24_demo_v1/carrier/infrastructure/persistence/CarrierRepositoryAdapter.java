package org.example.cafe24_demo_v1.carrier.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.carrier.domain.model.Carrier;
import org.example.cafe24_demo_v1.carrier.domain.repository.CarrierRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 도메인 인터페이스(CarrierRepository)의 JPA 구현체.
 */
@Repository
@RequiredArgsConstructor
public class CarrierRepositoryAdapter implements CarrierRepository {

    private final CarrierJpaRepository jpaRepository;
    private final CarrierMapper mapper;

    @Override
    public Optional<Carrier> findByMallIdAndShippingCarrierCode(String mallId, String shippingCarrierCode) {
        return jpaRepository.findByMallIdAndShippingCarrierCode(mallId, shippingCarrierCode)
                .map(mapper::toDomain);
    }

    @Override
    public void save(Carrier carrier) {
        CarrierEntity entity = mapper.toEntity(carrier);
        CarrierEntity saved = jpaRepository.save(entity);
        carrier.setId(saved.getId());
    }
}
