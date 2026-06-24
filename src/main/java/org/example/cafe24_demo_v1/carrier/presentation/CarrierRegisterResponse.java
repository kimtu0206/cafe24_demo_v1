package org.example.cafe24_demo_v1.carrier.presentation;

import org.example.cafe24_demo_v1.carrier.domain.model.Carrier;

import java.math.BigDecimal;

/**
 * 배송사 등록 응답 바디. Cafe24가 등록 후 채워준 carrierId/shippingCarrierName을 포함한다.
 */
public record CarrierRegisterResponse(
        Long carrierId,
        String shippingCarrierCode,
        String shippingCarrierName,
        String contact,
        String secondaryContact,
        String email,
        BigDecimal defaultShippingFee,
        String homepageUrl,
        String trackShipmentUrl
) {
    public static CarrierRegisterResponse from(Carrier carrier) {
        return new CarrierRegisterResponse(
                carrier.getCarrierId(),
                carrier.getShippingCarrierCode(),
                carrier.getShippingCarrierName(),
                carrier.getContact(),
                carrier.getSecondaryContact(),
                carrier.getEmail(),
                carrier.getDefaultShippingFee(),
                carrier.getHomepageUrl(),
                carrier.getTrackShipmentUrl()
        );
    }
}
