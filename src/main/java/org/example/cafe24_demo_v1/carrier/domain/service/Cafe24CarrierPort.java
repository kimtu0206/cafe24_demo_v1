package org.example.cafe24_demo_v1.carrier.domain.service;

import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.carrier.domain.model.Carrier;

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
}
