package org.example.cafe24_demo_v1.authorization.domain.model;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * OAuth 액세스 토큰과 리프레시 토큰을 묶은 불변(Immutable) Value Object.
 *
 * - 토큰 값이 바뀌면 새 객체를 생성한다 (기존 객체를 수정하지 않는다).
 * - 만료 여부 판단 로직을 이 클래스 안에 캡슐화해 외부에 흩어지지 않도록 한다.
 */
@Getter
public class TokenCredential {

    private final String accessToken;              // Cafe24 API 호출에 사용하는 단기 토큰
    private final String refreshToken;             // 액세스 토큰 재발급에 사용하는 장기 토큰
    private final String tokenType;                // 토큰 타입 (보통 "Bearer")
    private final LocalDateTime accessTokenExpiresAt;   // 액세스 토큰 만료 시각
    private final LocalDateTime refreshTokenExpiresAt;  // 리프레시 토큰 만료 시각

    public TokenCredential(
            String accessToken,
            String refreshToken,
            String tokenType,
            LocalDateTime accessTokenExpiresAt,
            LocalDateTime refreshTokenExpiresAt
    ) {
        Objects.requireNonNull(accessToken, "accessToken must not be null");
        Objects.requireNonNull(refreshToken, "refreshToken must not be null");
        Objects.requireNonNull(accessTokenExpiresAt, "accessTokenExpiresAt must not be null");
        Objects.requireNonNull(refreshTokenExpiresAt, "refreshTokenExpiresAt must not be null");
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.accessTokenExpiresAt = accessTokenExpiresAt;
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
    }

    /** 액세스 토큰이 만료됐는지 확인한다. */
    public boolean isAccessTokenExpired() {
        return LocalDateTime.now().isAfter(accessTokenExpiresAt);
    }

    /** 리프레시 토큰이 만료됐는지 확인한다. 만료되면 사용자가 다시 로그인해야 한다. */
    public boolean isRefreshTokenExpired() {
        return LocalDateTime.now().isAfter(refreshTokenExpiresAt);
    }

    /** 리프레시 토큰이 아직 유효해 액세스 토큰을 재발급 받을 수 있는지 확인한다. */
    public boolean canBeRefreshed() {
        return !isRefreshTokenExpired();
    }

    // 토큰 값이 같으면 동일한 자격증명으로 취급한다
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TokenCredential that)) return false;
        return Objects.equals(accessToken, that.accessToken)
                && Objects.equals(refreshToken, that.refreshToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessToken, refreshToken);
    }
}
