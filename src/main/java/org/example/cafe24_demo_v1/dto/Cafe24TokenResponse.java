package org.example.cafe24_demo_v1.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Cafe24TokenResponse {

    @JsonProperty("access_token")
    private String accessToken;

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