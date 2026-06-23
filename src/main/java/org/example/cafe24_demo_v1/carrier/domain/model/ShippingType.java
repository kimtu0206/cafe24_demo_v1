package org.example.cafe24_demo_v1.carrier.domain.model;

/**
 * Cafe24 배송사의 배송 가능 지역 구분.
 * Cafe24 API는 A(국내)/B(국내+해외)/C(해외)/F(설정안함) 코드로 표현한다.
 */
public enum ShippingType {
    DOMESTIC,
    DOMESTIC_AND_INTERNATIONAL,
    INTERNATIONAL,
    NOT_SET;

    public static ShippingType from(String code) {
        return switch (code) {
            case null -> NOT_SET; // Cafe24가 미설정 배송사에 대해 필드 자체를 비워서 응답하는 경우가 있음
            case "A" -> DOMESTIC;
            case "B" -> DOMESTIC_AND_INTERNATIONAL;
            case "C" -> INTERNATIONAL;
            case "F" -> NOT_SET;
            default -> throw new IllegalArgumentException("알 수 없는 ShippingType 코드입니다: " + code);
        };
    }

    public String toCode() {
        return switch (this) {
            case DOMESTIC -> "A";
            case DOMESTIC_AND_INTERNATIONAL -> "B";
            case INTERNATIONAL -> "C";
            case NOT_SET -> "F";
        };
    }
}
