package org.example.cafe24_demo_v1.carrier.domain.repository;

import org.example.cafe24_demo_v1.carrier.domain.model.Carrier;

import java.util.Optional;

/**
 * Carrier 저장소 포트(인터페이스). 구현체는 infrastructure 레이어가 담당한다.
 */
public interface CarrierRepository {

    Optional<Carrier> findByMallIdAndShippingCarrierCode(String mallId, String shippingCarrierCode);

    void save(Carrier carrier);
}
