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
}
