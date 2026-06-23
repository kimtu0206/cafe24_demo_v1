package org.example.cafe24_demo_v1.carrier.domain.service;

import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.carrier.domain.model.Carrier;

import java.util.List;
import java.util.Optional;

/**
 * Cafe24 Admin 배송사 API와 통신하는 외부 포트(Port) 인터페이스.
 *
 * 도메인 레이어에 위치하지만 구현체는 infrastructure 레이어의 Cafe24CarrierClient가 담당한다.
 * 도메인/애플리케이션 레이어는 이 인터페이스만 알고 실제 HTTP 통신 방식은 모른다.
 */
public interface Cafe24CarrierPort {

    /**
     * 배송사 코드로 Cafe24에 등록된 배송사 상세 정보를 조회한다.
     * Cafe24가 해당 코드를 찾지 못하면(404) 빈 Optional을 반환한다 — 그 외 오류는 예외로 전파된다.
     */
    Optional<Carrier> getCarrier(String mallId, String shippingCarrierCode, TokenCredential credential);

    /**
     * 등록된 배송사를 페이지 단위로 조회한다. Webhook 누락에 대비한 전체 동기화(CarrierService.syncFromCafe24)에 사용한다.
     */
    List<Carrier> getCarriers(String mallId, int offset, int limit, TokenCredential credential);
}
