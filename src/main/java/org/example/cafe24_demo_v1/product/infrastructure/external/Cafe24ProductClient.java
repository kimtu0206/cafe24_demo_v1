package org.example.cafe24_demo_v1.product.infrastructure.external;

import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.product.domain.model.Product;
import org.example.cafe24_demo_v1.product.domain.model.ProductRegistration;
import org.example.cafe24_demo_v1.product.domain.model.ProductStatus;
import org.example.cafe24_demo_v1.product.domain.service.Cafe24ProductPort;
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
 * Cafe24ProductPort의 실제 구현체 (Anti-Corruption Layer).
 *
 * Cafe24 Admin 상품 API와 HTTP 통신하는 인프라 레이어 어댑터.
 * 외부 API 응답(Cafe24ProductPayload)을 도메인 모델(Product)로 변환해 도메인에 전달한다.
 */
@Slf4j
@Component
public class Cafe24ProductClient implements Cafe24ProductPort {

    private final Cafe24Properties properties;
    private final RestTemplate restTemplate;

    public Cafe24ProductClient(Cafe24Properties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    @Override
    public Product createProduct(String mallId, ProductRegistration registration, TokenCredential credential) {
        String url = baseUrl(mallId) + "/products";
        ProductCreateRequest body = new ProductCreateRequest(registration);

        ProductCreateResponse response = exchange(
                mallId, url, HttpMethod.POST, new HttpEntity<>(body, headers(credential)), ProductCreateResponse.class
        );

        log.info("Product created: mallId={}, productNo={}", mallId, response.getProduct().getProductNo());
        return toDomain(mallId, response.getProduct());
    }

    @Override
    public Product getProduct(String mallId, Long productNo, TokenCredential credential) {
        String url = baseUrl(mallId) + "/products/" + productNo;

        ProductCreateResponse response = exchange(
                mallId, url, HttpMethod.GET, new HttpEntity<>(headers(credential)), ProductCreateResponse.class
        );

        return toDomain(mallId, response.getProduct());
    }

    @Override
    public Product updateProduct(String mallId, Long productNo, ProductRegistration registration, TokenCredential credential) {
        String url = baseUrl(mallId) + "/products/" + productNo;
        ProductUpdateRequest body = new ProductUpdateRequest(registration);

        ProductCreateResponse response = exchange(
                mallId, url, HttpMethod.PUT, new HttpEntity<>(body, headers(credential)), ProductCreateResponse.class
        );

        log.info("Product updated: mallId={}, productNo={}", mallId, productNo);
        return toDomain(mallId, response.getProduct());
    }

    @Override
    public void deleteProduct(String mallId, Long productNo, TokenCredential credential) {
        String url = UriComponentsBuilder.fromUriString(baseUrl(mallId) + "/products/" + productNo)
                .queryParam("shop_no", 1)
                .toUriString();

        exchange(mallId, url, HttpMethod.DELETE, new HttpEntity<>(headers(credential)), Void.class);
        log.info("Product deleted: mallId={}, productNo={}", mallId, productNo);
    }

    @Override
    public List<Product> getProducts(String mallId, int offset, int limit, TokenCredential credential) {
        String url = UriComponentsBuilder.fromUriString(baseUrl(mallId) + "/products")
                .queryParam("offset", offset)
                .queryParam("limit", limit)
                .toUriString();

        ProductListResponse response = exchange(
                mallId, url, HttpMethod.GET, new HttpEntity<>(headers(credential)), ProductListResponse.class
        );

        List<Cafe24ProductPayload> products = response.getProducts();
        if (products == null || products.isEmpty()) {
            return Collections.emptyList();
        }
        return products.stream()
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

    private <T> T exchange(String mallId, String url, HttpMethod method, HttpEntity<?> request, Class<T> responseType) {
        try {
            return restTemplate.exchange(url, method, request, responseType).getBody();
        } catch (HttpStatusCodeException e) {
            log.error("Cafe24 product API call failed: mallId={}, status={}", mallId, e.getStatusCode());
            log.debug("Cafe24 product API error body: {}", e.getResponseBodyAsString());
            throw new Cafe24ApiException(
                    "Cafe24 product API call failed. status=" + e.getStatusCode(),
                    e.getStatusCode(),
                    e.getResponseBodyAsString(),
                    e
            );
        }
    }

    /** Cafe24 API 응답 DTO → 도메인 모델 변환 */
    private Product toDomain(String mallId, Cafe24ProductPayload payload) {
        return Product.register(
                mallId,
                payload.getProductNo(),
                payload.getProductName(),
                payload.getPrice(),
                payload.getSupplyPrice(),
                ProductStatus.from(payload.getDisplay(), payload.getSelling()),
                payload.getDescription(),
                payload.getPaymentInfo(),
                payload.getShippingInfo(),
                payload.getExchangeInfo(),
                payload.getPriceExcludingTax(),
                payload.getDetailImage(),
                payload.getImageUploadType()
        );
    }
}
