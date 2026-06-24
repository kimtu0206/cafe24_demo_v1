package org.example.cafe24_demo_v1.carrier.infrastructure.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Cafe24 배송사 등록 API 응답의 "carrier" 내부 필드 DTO.
 * shipping_type은 최상위가 아니라 shipping_fee_setting_detail 안에 들어있다(GET 목록 응답과 다른 구조).
 * shipping_fee_setting_detail은 등록 시 항상 미설정("F")으로 보내므로 응답에서도 비어있을 수 있다.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class CarrierCreateResponsePayload {

    @JsonProperty("carrier_id")
    private Long carrierId;

    @JsonProperty("shipping_carrier_code")
    private String shippingCarrierCode;

    @JsonProperty("shipping_carrier")
    private String shippingCarrierName;

    private String contact;

    @JsonProperty("secondary_contact")
    private String secondaryContact;

    private String email;

    @JsonProperty("default_shipping_fee")
    private BigDecimal defaultShippingFee;

    @JsonProperty("homepage_url")
    private String homepageUrl;

    @JsonProperty("track_shipment_url")
    private String trackShipmentUrl;

    @JsonProperty("shipping_fee_setting")
    private String shippingFeeSetting;

    @JsonProperty("shipping_fee_setting_detail")
    private ShippingFeeSettingDetail shippingFeeSettingDetail;
}
