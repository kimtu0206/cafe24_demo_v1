package org.example.cafe24_demo_v1.authorization.infrastructure.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

/**
 * Cafe24 OAuth 토큰 API 응답을 역직렬화하는 DTO.
 *
 * infrastructure 레이어 내부에서만 사용하며, 도메인 레이어로 직접 노출되지 않는다.
 * Cafe24OAuthGateway가 이 DTO를 도메인 모델(TokenCredential)로 변환해 반환한다.
 * @JsonIgnoreProperties: API 응답에 알 수 없는 필드가 추가돼도 역직렬화 오류 없이 무시한다.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class Cafe24TokenResponse {

    @ToString.Exclude
    @JsonProperty("access_token")
    private String accessToken;

    @ToString.Exclude
    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("expires_at")
    private String expiresAt;

    @JsonProperty("refresh_token_expires_at")
    private String refreshTokenExpiresAt;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("mall_id")
    private String mallId;
}
