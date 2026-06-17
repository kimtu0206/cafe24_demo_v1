package org.example.cafe24_demo_v1.authorization.domain.model;

import java.util.Objects;

/**
 * 인가(AppAuthorization)를 식별하는 복합 Value Object.
 * <p>
 * mallId(쇼핑몰)와 clientId(앱)의 조합으로 하나의 인가를 고유하게 식별한다.
 * Value Object이므로 생성 후 값을 변경할 수 없고, 값이 같으면 같은 객체로 취급한다.
 *
 * @param mallId   Cafe24 쇼핑몰 ID (예: "mymall")
 * @param clientId OAuth 앱 클라이언트 ID
 */
public record AuthorizationId(String mallId, String clientId) {

    public AuthorizationId {
        Objects.requireNonNull(mallId, "mallId must not be null");
        Objects.requireNonNull(clientId, "clientId must not be null");
    }

    // Value Object는 필드 값으로 동등성을 판단한다 (참조가 아닌 내용 비교)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthorizationId that)) return false;
        return Objects.equals(mallId, that.mallId) && Objects.equals(clientId, that.clientId);
    }

    @Override
    public String toString() {
        return "AuthorizationId{mallId='" + mallId + "', clientId='" + clientId + "'}";
    }
}
