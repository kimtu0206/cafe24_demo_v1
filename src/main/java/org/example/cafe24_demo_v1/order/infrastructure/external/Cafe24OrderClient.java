package org.example.cafe24_demo_v1.order.infrastructure.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.order.domain.model.Order;
import org.example.cafe24_demo_v1.order.domain.model.OrderEmbeddedResources;
import org.example.cafe24_demo_v1.order.domain.service.Cafe24OrderPort;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Cafe24OrderPort의 실제 구현체 (Anti-Corruption Layer).
 *
 * 주문 응답은 컬럼화하기엔 필드가 너무 많고 Cafe24 스펙 변경 가능성도 있어,
 * Cafe24ProductPayload처럼 엄격한 DTO로 매핑하지 않는다. 대신 JsonNode에서 핵심 필드만
 * 직접 꺼내 쓰고, 응답 원본(JSON)은 그대로 보존해 Order.rawJson에 저장한다.
 */
@Slf4j
@Component
public class Cafe24OrderClient implements Cafe24OrderPort {

    private static final DateTimeFormatter SEARCH_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 주문 하위 리소스(품목/수령자/주문자/반품/취소/교환)를 한 번의 호출로 같이 조회하기 위한 embed 값.
     * 각 리소스는 별도 엔드포인트(예: /orders/{order_id}/items)로 조회했을 때와 동일한 형태로 응답에 포함되며,
     * 컬럼화하지 않고 rawJson에 원본 그대로 보존한다(필드 구조 변경에 안전하게 대응하기 위함).
     */
    private static final String EMBED_RESOURCES = "items,receivers,buyer,return,cancellation,exchange";

    private final Cafe24Properties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public Cafe24OrderClient(Cafe24Properties properties, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Order> getOrders(String mallId, LocalDateTime updatedSince, int offset, int limit, TokenCredential credential) {
        return getOrders(mallId, updatedSince.toLocalDate(), LocalDate.now(), offset, limit, credential);
    }

    @Override
    public List<Order> getOrders(String mallId, LocalDate startDate, LocalDate endDate, int offset, int limit, TokenCredential credential) {
        String url = UriComponentsBuilder.fromUriString(baseUrl(mallId) + "/orders")
                .queryParam("start_date", startDate.format(SEARCH_DATE_FORMAT))
                .queryParam("end_date", endDate.format(SEARCH_DATE_FORMAT))
                .queryParam("offset", offset)
                .queryParam("limit", limit)
                .queryParam("embed", EMBED_RESOURCES)
                .toUriString();

        String body = exchange(url, new HttpEntity<>(headers(credential)));
        return parseOrders(mallId, body);
    }

    @Override
    public Optional<Order> getOrder(String mallId, String orderId, TokenCredential credential) {
        // 주문 상세 단건 엔드포인트의 응답 구조가 불확실해, 이미 검증된 목록 조회를 order_id로 필터링해서 재사용한다.
        String url = UriComponentsBuilder.fromUriString(baseUrl(mallId) + "/orders")
                .queryParam("order_id", orderId)
                .queryParam("embed", EMBED_RESOURCES)
                .toUriString();

        String body = exchange(url, new HttpEntity<>(headers(credential)));
        return parseOrders(mallId, body).stream().findFirst();
    }

    private List<Order> parseOrders(String mallId, String body) {
        JsonNode root = readTree(body);
        List<Order> orders = new ArrayList<>();
        for (JsonNode node : root.path("orders")) {
            orders.add(toDomain(mallId, node));
        }
        return orders;
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

    private String exchange(String url, HttpEntity<?> request) {
        try {
            return restTemplate.exchange(url, HttpMethod.GET, request, String.class).getBody();
        } catch (HttpStatusCodeException e) {
            log.error("Cafe24 order API call failed: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new Cafe24ApiException(
                    "Cafe24 order API call failed. status=" + e.getStatusCode(),
                    e.getStatusCode(),
                    e.getResponseBodyAsString(),
                    e
            );
        }
    }

    private JsonNode readTree(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (JsonProcessingException e) {
            throw new Cafe24ApiException("Cafe24 order 응답 파싱 실패", null, body, e);
        }
    }

    /**
     * Cafe24 API 응답(JsonNode) → 도메인 모델 변환. 원본 노드는 그대로 직렬화해 rawJson으로 보존한다.
     *
     * orderStatus: Cafe24 주문 목록 응답에는 단일 order_status 필드가 없고 paid/canceled/shipping_status로
     * 나뉘어 있다. 세 값을 조합해 하나의 상태로 만드는 규칙은 아직 정해지지 않아 우선 null로 둔다
     * (raw json에는 세 값이 모두 보존되어 있어 이후 규칙이 정해지면 재처리로 채울 수 있다).
     *
     * buyerName: embed=buyer로 주문자정보를 함께 요청하지만, Cafe24의 개인정보 제공 동의 승인이 있어야
     * 응답에 실제 값이 채워진다. 동의 전이거나 embed 응답의 정확한 필드 구조가 확인되지 않아 우선 null로 둔다
     * (embed로 받은 buyer/items/receivers/return/cancellation/exchange 원본은 rawJson에 보존되므로,
     * 구조가 확인되면 이후 컬럼 매핑을 추가할 수 있다). buyerEmail은 대신 member_email로 채운다(비회원 주문이면 비어있을 수 있음).
     */
    private Order toDomain(String mallId, JsonNode node) {
        return Order.register(
                mallId,
                text(node, "order_id"),
                text(node, "order_status"),
                text(node, "member_id"),
                text(node, "buyer_name"),
                text(node, "member_email"),
                decimal(node, "payment_amount"),
                joinIfArray(node, "payment_method"),
                dateTime(node, "order_date"),
                node.toString(),
                extractEmbeds(node)
        );
    }

    /**
     * EMBED_RESOURCES에 나열된 하위 리소스 이름을 순회하며, 응답에 포함된 해당 리소스의 원본(JSON)을
     * 같은 이름의 컬럼에 매핑한다. 리소스가 응답에 없으면(개인정보 동의 미승인 등) null로 둔다.
     */
    private OrderEmbeddedResources extractEmbeds(JsonNode node) {
        String items = null;
        String receivers = null;
        String buyer = null;
        String returnInfo = null;
        String cancellation = null;
        String exchange = null;

        for (String resource : EMBED_RESOURCES.split(",")) {
            JsonNode embedded = node.get(resource);
            String json = (embedded == null || embedded.isNull()) ? null : embedded.toString();
            switch (resource) {
                case "items" -> items = json;
                case "receivers" -> receivers = json;
                case "buyer" -> buyer = json;
                case "return" -> returnInfo = json;
                case "cancellation" -> cancellation = json;
                case "exchange" -> exchange = json;
            }
        }

        return new OrderEmbeddedResources(items, receivers, buyer, returnInfo, cancellation, exchange);
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return (value == null || value.isNull()) ? null : value.asText();
    }

    /** payment_method처럼 배열로 오는 필드를 콤마로 합쳐 하나의 문자열로 저장한다. */
    private String joinIfArray(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isArray()) {
            return value.asText();
        }
        List<String> items = new ArrayList<>();
        value.forEach(item -> items.add(item.asText()));
        return String.join(",", items);
    }

    private BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            return null;
        }
        return new BigDecimal(value.asText());
    }

    private LocalDateTime dateTime(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            return null;
        }
        return OffsetDateTime.parse(value.asText()).toLocalDateTime();
    }
}
