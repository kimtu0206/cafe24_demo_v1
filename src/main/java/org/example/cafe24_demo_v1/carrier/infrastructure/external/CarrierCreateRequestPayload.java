package org.example.cafe24_demo_v1.carrier.infrastructure.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Cafe24 배송사 등록 API 요청의 "request" 내부 필드 DTO.
 * shipping_carrier(배송사명)는 Cafe24가 shipping_carrier_code 기준으로 채워 응답하므로 보내지 않는다(null).
 * 배송비 상세 설정(구간별/해외배송 등)은 이 요청 범위 밖이라 shipping_fee_setting을 항상 "F"(미설정)로 고정한다.
 * 요청 직렬화 전용 DTO라 응답 역직렬화에 필요한 setter는 두지 않는다.
 */
@Getter
class CarrierCreateRequestPayload {

    @JsonProperty("shipping_carrier_code")
    private final String shippingCarrierCode;

    @JsonProperty("shipping_carrier")
    private final String shippingCarrier = null;

    private final String contact;

    @JsonProperty("secondary_contact")
    private final String secondaryContact;

    private final String email;

    @JsonProperty("default_shipping_fee")
    private final BigDecimal defaultShippingFee;

    @JsonProperty("homepage_url")
    private final String homepageUrl;

    @JsonProperty("track_shipment_url")
    private final String trackShipmentUrl;

    @JsonProperty("shipping_fee_setting")
    private final String shippingFeeSetting = "F";

    CarrierCreateRequestPayload(
            String shippingCarrierCode,
            String contact,
            String secondaryContact,
            String email,
            BigDecimal defaultShippingFee,
            String homepageUrl,
            String trackShipmentUrl
    ) {
        this.shippingCarrierCode = shippingCarrierCode;
        this.contact = contact;
        this.secondaryContact = secondaryContact;
        this.email = email;
        this.defaultShippingFee = defaultShippingFee;
        this.homepageUrl = homepageUrl;
        this.trackShipmentUrl = trackShipmentUrl;
    }
}
