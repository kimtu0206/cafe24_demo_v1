package org.example.cafe24_demo_v1.carrier.domain.service;

import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.carrier.domain.model.Carrier;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cafe24 Admin 배송사 API와 통신하는 외부 포트(Port) 인터페이스.
 *
 * 도메인 레이어에 위치하지만 구현체는 infrastructure 레이어의 Cafe24CarrierClient가 담당한다.
 * 도메인/애플리케이션 레이어는 이 인터페이스만 알고 실제 HTTP 통신 방식은 모른다.
 */
public interface Cafe24CarrierPort {

    /**
     * 등록된 배송사를 페이지 단위로 조회한다. Webhook 누락에 대비한 전체 동기화(CarrierService.syncFromCafe24)에 사용한다.
     */
    List<Carrier> getCarriers(String mallId, int offset, int limit, TokenCredential credential);

    /**
     * Cafe24에 새 배송사를 등록한다. shippingCarrierCode는 Cafe24에 사전 등록된 배송사 코드여야 하며,
     * 배송사명은 Cafe24가 코드 기준으로 채워 응답하므로 별도로 전달하지 않는다.
     * 배송비 상세 설정(구간별/해외배송 등)은 이 메서드의 범위 밖이며 항상 미설정 상태로 등록된다.
     */
    Carrier createCarrier(
            String mallId,
            String shippingCarrierCode,
            String contact,
            String secondaryContact,
            String email,
            BigDecimal defaultShippingFee,
            String homepageUrl,
            String trackShipmentUrl,
            TokenCredential credential
    );
}
