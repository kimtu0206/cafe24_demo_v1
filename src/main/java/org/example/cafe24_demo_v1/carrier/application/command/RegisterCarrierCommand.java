package org.example.cafe24_demo_v1.carrier.application.command;

import java.math.BigDecimal;

/**
 * 배송사 등록 유즈케이스에 전달하는 커맨드 객체.
 *
 * Controller가 HTTP 요청을 이 객체로 변환해 CarrierService에 전달한다.
 *
 * @param mallId             Cafe24 쇼핑몰 ID
 * @param shippingCarrierCode Cafe24에 사전 등록된 배송사 코드 (필수)
 */
public record RegisterCarrierCommand(
        String mallId,
        String shippingCarrierCode,
        String contact,
        String secondaryContact,
        String email,
        BigDecimal defaultShippingFee,
        String homepageUrl,
        String trackShipmentUrl
) {}
