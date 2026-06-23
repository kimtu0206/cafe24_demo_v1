package org.example.cafe24_demo_v1.carrier.infrastructure.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Cafe24 Admin 배송사 API가 반환하는 "carrier" 객체를 역직렬화하는 DTO.
 * infrastructure 레이어 내부에서만 사용하며, 도메인 레이어로 직접 노출되지 않는다.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class Cafe24CarrierPayload {

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

    @JsonProperty("track_shipment_url")
    private String trackShipmentUrl;

    @JsonProperty("default_shipping_fee")
    private BigDecimal defaultShippingFee;

    @JsonProperty("homepage_url")
    private String homepageUrl;

    @JsonProperty("shipping_type")
    private String shippingType;

    @JsonProperty("default_shipping_carrier")
    private String defaultShippingCarrier;

    @JsonProperty("shipping_fee_setting")
    private String shippingFeeSetting;
}
