package org.example.cafe24_demo_v1.carrier.infrastructure.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Cafe24 배송사 등록 응답의 "shipping_fee_setting_detail" 중 shipping_type만 추출하기 위한 DTO.
 * 구간별/해외배송/카테고리별 배송비 등 나머지 하위 필드는 이 애플리케이션의 범위 밖이라 무시한다.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class ShippingFeeSettingDetail {

    @JsonProperty("shipping_type")
    private String shippingType;
}
