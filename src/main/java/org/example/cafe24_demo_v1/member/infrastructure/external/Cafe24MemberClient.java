package org.example.cafe24_demo_v1.member.infrastructure.external;

import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.member.domain.model.Member;
import org.example.cafe24_demo_v1.member.domain.service.Cafe24MemberPort;
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.example.cafe24_demo_v1.shared.exception.Cafe24ApiException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class Cafe24MemberClient implements Cafe24MemberPort {

    private final Cafe24Properties properties;
    private final RestTemplate restTemplate;

    public Cafe24MemberClient(Cafe24Properties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    @Override
    public List<Member> searchMembers(String mallId, String memberIds, String cellphone, TokenCredential credential) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(baseUrl(mallId) + "/customers");

        if (StringUtils.hasText(memberIds)) {
            String normalized = Arrays.stream(memberIds.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining(","));
            builder.queryParam("member_id", normalized);
        }
        if (StringUtils.hasText(cellphone)) {
            builder.queryParam("cellphone", cellphone);
        }

        String url = builder.toUriString();

        Cafe24CustomerListResponse response = exchange(
                mallId, url, HttpMethod.GET, new HttpEntity<>(headers(credential)), Cafe24CustomerListResponse.class
        );

        List<Cafe24CustomerPayload> customers = response.getCustomers();
        if (customers == null || customers.isEmpty()) {
            return Collections.emptyList();
        }

        return customers.stream().map(this::toDomain).toList();
    }

    private String baseUrl(String mallId) {
        return "https://" + mallId + ".cafe24api.com/api/v2/admin";
    }

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
            log.error("Cafe24 member API call failed: mallId={}, status={}", mallId, e.getStatusCode());
            log.debug("Cafe24 member API error body: {}", e.getResponseBodyAsString());
            throw new Cafe24ApiException(
                    "Cafe24 member API call failed. status=" + e.getStatusCode(),
                    e.getStatusCode(),
                    e.getResponseBodyAsString(),
                    e
            );
        }
    }

    private Member toDomain(Cafe24CustomerPayload payload) {
        return new Member(
                payload.getShopNo(),
                payload.getMemberId(),
                payload.getMemberName(),
                payload.getGroupNo(),
                payload.getMemberAuthentication(),
                payload.getUseBlacklist(),
                payload.getBlacklistType(),
                payload.getAuthenticationMethod(),
                payload.getSms(),
                payload.getNewsMail(),
                payload.getSolarCalendar(),
                payload.getTotalPoints(),
                payload.getAvailablePoints(),
                payload.getUsedPoints(),
                payload.getUseMobileApp(),
                payload.getAvailableCredits(),
                payload.getFixedGroup(),
                payload.getGender(),
                payload.getEmail(),
                payload.getCellphone(),
                payload.getPhone(),
                payload.getBirthday(),
                payload.getCreatedDate() != null ? payload.getCreatedDate().toLocalDateTime() : null,
                payload.getLastLoginDate() != null ? payload.getLastLoginDate().toLocalDateTime() : null
        );
    }
}
