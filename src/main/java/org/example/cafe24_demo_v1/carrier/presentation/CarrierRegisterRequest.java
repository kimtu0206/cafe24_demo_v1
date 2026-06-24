package org.example.cafe24_demo_v1.carrier.presentation;

import java.math.BigDecimal;

/**
 * 배송사 등록 요청 바디.
 * shippingCarrierCode는 Cafe24에 사전 등록된 배송사 코드여야 하며, 배송사명은 입력받지 않는다
 * (Cafe24가 코드 기준으로 채워 응답한다).
 */
public record CarrierRegisterRequest(
        String shippingCarrierCode,
        String contact,
        String secondaryContact,
        String email,
        BigDecimal defaultShippingFee,
        String homepageUrl,
        String trackShipmentUrl
) {}
