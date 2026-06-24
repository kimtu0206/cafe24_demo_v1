package org.example.cafe24_demo_v1.carrier.infrastructure.external;

import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.carrier.domain.model.Carrier;
import org.example.cafe24_demo_v1.carrier.domain.model.ShippingType;
import org.example.cafe24_demo_v1.carrier.domain.service.Cafe24CarrierPort;
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.example.cafe24_demo_v1.shared.exception.Cafe24ApiException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Cafe24CarrierPort의 실제 구현체 (Anti-Corruption Layer).
 *
 * Cafe24 Admin 배송사 API와 HTTP 통신하는 인프라 레이어 어댑터.
 * 외부 API 응답(Cafe24CarrierPayload)을 도메인 모델(Carrier)로 변환해 도메인에 전달한다.
 */
@Slf4j
@Component
public class Cafe24CarrierClient implements Cafe24CarrierPort {

    private final Cafe24Properties properties;
    private final RestTemplate restTemplate;

    public Cafe24CarrierClient(Cafe24Properties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    /**
     * 등록된 배송사를 offset/limit 페이지 단위로 조회한다.
     * Cafe24 배송사 목록 API는 변경 시점 필터(updated_since 등)를 제공하지 않으므로,
     * 호출할 때마다 전체를 페이지 단위로 다시 조회하는 방식으로 동작한다.
     */
    @Override
    public List<Carrier> getCarriers(String mallId, int offset, int limit, TokenCredential credential) {
        String url = UriComponentsBuilder.fromUriString(baseUrl(mallId) + "/carriers")
                .queryParam("offset", offset)
                .queryParam("limit", limit)
                .toUriString();

        CarrierListResponse response = exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers(credential)), CarrierListResponse.class
        );

        List<Cafe24CarrierPayload> carriers = response.getCarriers();
        if (carriers == null || carriers.isEmpty()) {
            return Collections.emptyList();
        }
        return carriers.stream()
                .map(payload -> toDomain(mallId, payload))
                .collect(Collectors.toList());
    }

    /**
     * Cafe24에 사전 등록된 배송사 코드를 기준으로 새 배송사를 등록한다.
     * 배송사명은 Cafe24가 코드로부터 채워 응답하고, 배송비 상세 설정은 항상 미설정("F")으로 보낸다.
     */
    @Override
    public Carrier createCarrier(
            String mallId,
            String shippingCarrierCode,
            String contact,
            String secondaryContact,
            String email,
            BigDecimal defaultShippingFee,
            String homepageUrl,
            String trackShipmentUrl,
            TokenCredential credential
    ) {
        String url = baseUrl(mallId) + "/carriers";
        CarrierCreateRequestPayload payload = new CarrierCreateRequestPayload(
                shippingCarrierCode, contact, secondaryContact, email, defaultShippingFee, homepageUrl, trackShipmentUrl
        );
        CarrierCreateRequest body = new CarrierCreateRequest(payload);

        CarrierCreateResponse response = exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, headers(credential)), CarrierCreateResponse.class
        );
        return toDomain(mallId, response.getCarrier());
    }

    private String baseUrl(String mallId) {
        return "https://" + mallId + ".cafe24api.com/api/v2/admin";
    }

    /** Cafe24 Admin API 인증 헤더. Bearer 액세스 토큰 + API 버전을 함께 전달한다. */
    private HttpHeaders headers(TokenCredential credential) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(credential.getAccessToken());
        headers.set("X-Cafe24-Api-Version", properties.getApiVersion());
        return headers;
    }

    private <T> T exchange(String url, HttpMethod method, HttpEntity<?> request, Class<T> responseType) {
        try {
            return restTemplate.exchange(url, method, request, responseType).getBody();
        } catch (HttpStatusCodeException e) {
            log.error("Cafe24 carrier API call failed: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new Cafe24ApiException(
                    "Cafe24 carrier API call failed. status=" + e.getStatusCode(),
                    e.getStatusCode(),
                    e.getResponseBodyAsString(),
                    e
            );
        }
    }

    /** Cafe24 API 응답 DTO → 도메인 모델 변환. */
    private Carrier toDomain(String mallId, Cafe24CarrierPayload payload) {
        return Carrier.register(
                mallId,
                payload.getCarrierId(),
                payload.getShippingCarrierCode(),
                payload.getShippingCarrierName(),
                payload.getContact(),
                payload.getSecondaryContact(),
                payload.getEmail(),
                payload.getTrackShipmentUrl(),
                payload.getDefaultShippingFee(),
                payload.getHomepageUrl(),
                ShippingType.from(payload.getShippingType()),
                "T".equals(payload.getDefaultShippingCarrier()),
                "T".equals(payload.getShippingFeeSetting())
        );
    }

    /**
     * 배송사 등록 응답 DTO → 도메인 모델 변환.
     * shipping_type이 shipping_fee_setting_detail 안에 있고, 미설정 등록 시 detail 자체가 없을 수 있다.
     * default_carrier는 등록 응답에 없는 값이라 false로 두고, 추후 CarrierSyncScheduler의 전체 재동기화가 실제 값으로 갱신한다.
     */
    private Carrier toDomain(String mallId, CarrierCreateResponsePayload payload) {
        ShippingFeeSettingDetail detail = payload.getShippingFeeSettingDetail();
        String shippingTypeCode = detail != null ? detail.getShippingType() : null;
        return Carrier.register(
                mallId,
                payload.getCarrierId(),
                payload.getShippingCarrierCode(),
                payload.getShippingCarrierName(),
                payload.getContact(),
                payload.getSecondaryContact(),
                payload.getEmail(),
                payload.getTrackShipmentUrl(),
                payload.getDefaultShippingFee(),
                payload.getHomepageUrl(),
                ShippingType.from(shippingTypeCode),
                false,
                "T".equals(payload.getShippingFeeSetting())
        );
    }
}
