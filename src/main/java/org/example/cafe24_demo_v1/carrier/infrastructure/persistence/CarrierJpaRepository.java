package org.example.cafe24_demo_v1.carrier.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Carrier JPA 레포지토리.
 */
interface CarrierJpaRepository extends JpaRepository<CarrierEntity, Long> {

    Optional<CarrierEntity> findByMallIdAndShippingCarrierCode(String mallId, String shippingCarrierCode);
}
