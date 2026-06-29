package org.example.cafe24_demo_v1.carrier.presentation;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * 배송사 등록 요청 바디.
 * shippingCarrierCode는 Cafe24에 사전 등록된 배송사 코드여야 하며, 배송사명은 입력받지 않는다
 * (Cafe24가 코드 기준으로 채워 응답한다).
 */
public record CarrierRegisterRequest(
        @Schema(example = "0001") String shippingCarrierCode,
        @Schema(example = "02-1234-5678") String contact,
        @Schema(example = "02-8765-4321") String secondaryContact,
        @Schema(example = "support@carrier.co.kr") String email,
        @Schema(example = "3000") BigDecimal defaultShippingFee,
        @Schema(example = "https://www.carrier.co.kr") String homepageUrl,
        @Schema(example = "https://www.carrier.co.kr/track?no={invoice_no}") String trackShipmentUrl
) {}
