package org.example.cafe24_demo_v1.authorization.infrastructure.external;

import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.authorization.domain.service.Cafe24OAuthPort;
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/**
 * Cafe24OAuthPort의 실제 구현체 (Anti-Corruption Layer).
 *
 * Cafe24 OAuth API와 HTTP 통신하는 인프라 레이어 어댑터.
 * 외부 API 응답(Cafe24TokenResponse)을 도메인 모델(TokenCredential)로 변환해 도메인에 전달한다.
 * 도메인 레이어는 이 클래스의 존재를 모르고, 인터페이스(Cafe24OAuthPort)만 안다.
 */
@Slf4j
@Component
public class Cafe24OAuthGateway implements Cafe24OAuthPort {

    private final Cafe24Properties properties;
    private final RestTemplate restTemplate;

    public Cafe24OAuthGateway(Cafe24Properties properties) {
        this.properties = properties;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Authorization Code를 Cafe24 서버에 전달해 최초 토큰을 발급받는다.
     * OAuth 2.0 Authorization Code Grant Flow.
     */
    @Override
    public TokenCredential issueToken(String authorizationCode) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", authorizationCode);
        body.add("redirect_uri", properties.getRedirectUri());

        Cafe24TokenResponse response = requestToken(body);
        log.info("Token issued for mallId={}", response.getMallId());
        return toCredential(response);
    }

    /**
     * 리프레시 토큰으로 만료된 액세스 토큰을 재발급받는다.
     * OAuth 2.0 Refresh Token Grant Flow.
     */
    @Override
    public TokenCredential refreshToken(String refreshToken) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", refreshToken);

        Cafe24TokenResponse response = requestToken(body);
        log.info("Token refreshed for mallId={}", response.getMallId());
        return toCredential(response);
    }

    /**
     * Cafe24 토큰 엔드포인트에 POST 요청을 보내는 공통 메서드.
     * Basic Auth 헤더에 clientId:clientSecret 을 담아 전송한다.
     */
    private Cafe24TokenResponse requestToken(MultiValueMap<String, String> body) {
        String tokenUrl = "https://" + properties.getMallId() + ".cafe24api.com/api/v2/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(properties.getClientId(), properties.getClientSecret());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        try {
            return restTemplate.postForEntity(tokenUrl, request, Cafe24TokenResponse.class).getBody();
        } catch (HttpClientErrorException e) {
            throw new IllegalStateException("Cafe24 token request failed. status=" + e.getStatusCode(), e);
        }
    }

    /** Cafe24 API 응답 DTO → 도메인 Value Object 변환 */
    private TokenCredential toCredential(Cafe24TokenResponse response) {
        return new TokenCredential(
                response.getAccessToken(),
                response.getRefreshToken(),
                response.getTokenType(),
                parseDateTime(response.getExpiresAt()),
                parseDateTime(response.getRefreshTokenExpiresAt())
        );
    }

    /**
     * Cafe24가 반환하는 날짜 문자열을 LocalDateTime으로 변환한다.
     * Cafe24는 "2024-01-01T00:00:00" 또는 "2024-01-01T00:00:00+09:00" 두 형식을 혼용한다.
     */
    private LocalDateTime parseDateTime(String value) {
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            // 타임존 오프셋이 포함된 경우 OffsetDateTime으로 먼저 파싱 후 변환
            return OffsetDateTime.parse(value).toLocalDateTime();
        }
    }
}
