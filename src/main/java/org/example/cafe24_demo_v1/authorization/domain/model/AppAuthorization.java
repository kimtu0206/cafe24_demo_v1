package org.example.cafe24_demo_v1.authorization.domain.model;

import lombok.Getter;
import org.example.cafe24_demo_v1.authorization.domain.event.AuthorizationGranted;
import org.example.cafe24_demo_v1.authorization.domain.event.AuthorizationRefreshed;
import org.example.cafe24_demo_v1.authorization.domain.event.AuthorizationRevoked;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Cafe24 앱이 특정 쇼핑몰로부터 받은 OAuth 인가(Authorization)를 나타내는 Aggregate Root.
 *
 * - 인가의 생명주기(발급 → 갱신 → 취소)를 이 클래스 안에서만 관리한다.
 * - 외부에서 필드를 직접 수정하지 못하도록 setter를 열지 않는다.
 * - 상태가 바뀔 때마다 도메인 이벤트를 등록해 다른 컨텍스트에 알릴 수 있다.
 */
@Getter
public class AppAuthorization extends AbstractAggregateRoot<AppAuthorization> {

    private Long id;                        // DB PK (영속화 후 채워짐)
    private AuthorizationId authorizationId; // 인가를 식별하는 복합 키 (mallId + clientId)
    private TokenCredential credential;      // 현재 유효한 토큰 자격증명
    private AuthorizationStatus status;      // ACTIVE 또는 REVOKED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 외부에서 new AppAuthorization() 직접 생성 금지 — 반드시 팩토리 메서드 사용
    private AppAuthorization() {}

    /**
     * 최초 인가 발급 시 사용하는 팩토리 메서드.
     * 생성 즉시 AuthorizationGranted 이벤트를 등록한다.
     */
    public static AppAuthorization grant(AuthorizationId authorizationId, TokenCredential credential) {
        AppAuthorization auth = new AppAuthorization();
        auth.authorizationId = authorizationId;
        auth.credential = credential;
        auth.status = AuthorizationStatus.ACTIVE;
        auth.createdAt = LocalDateTime.now();
        auth.updatedAt = LocalDateTime.now();
        auth.registerEvent(new AuthorizationGranted(authorizationId));
        return auth;
    }

    /**
     * DB에서 조회한 데이터로 도메인 객체를 복원할 때 사용하는 팩토리 메서드.
     * 이벤트를 등록하지 않는다 (이미 발생한 과거 사건이므로).
     */
    public static AppAuthorization reconstitute(
            Long id,
            AuthorizationId authorizationId,
            TokenCredential credential,
            AuthorizationStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        AppAuthorization auth = new AppAuthorization();
        auth.id = id;
        auth.authorizationId = authorizationId;
        auth.credential = credential;
        auth.status = status;
        auth.createdAt = createdAt;
        auth.updatedAt = updatedAt;
        return auth;
    }

    /**
     * 액세스 토큰을 새 자격증명으로 교체한다.
     * 이미 취소된(REVOKED) 인가는 갱신할 수 없다.
     */
    public void refresh(TokenCredential newCredential) {
        Objects.requireNonNull(newCredential);
        if (this.status == AuthorizationStatus.REVOKED) {
            throw new IllegalStateException("Cannot refresh a revoked authorization");
        }
        this.credential = newCredential;
        this.updatedAt = LocalDateTime.now();
        registerEvent(new AuthorizationRefreshed(authorizationId));
    }

    /**
     * 인가를 취소한다. 앱 삭제 Webhook 수신 시 호출된다.
     * 취소 후에는 refresh()나 getValidCredential()을 호출할 수 없다.
     */
    public void revoke() {
        this.status = AuthorizationStatus.REVOKED;
        this.updatedAt = LocalDateTime.now();
        registerEvent(new AuthorizationRevoked(authorizationId));
    }

    /** 액세스 토큰 만료 여부를 확인한다. */
    public boolean isExpired() {
        return credential.isAccessTokenExpired();
    }

    /** 액세스 토큰은 만료됐지만 리프레시 토큰으로 갱신 가능한 상태인지 확인한다. */
    public boolean needsRefresh() {
        return credential.isAccessTokenExpired() && credential.canBeRefreshed();
    }

    /**
     * API 호출에 사용할 유효한 자격증명을 반환한다.
     * REVOKED 상태라면 예외를 던진다.
     */
    public TokenCredential getValidCredential() {
        if (status == AuthorizationStatus.REVOKED) {
            throw new IllegalStateException("Authorization has been revoked");
        }
        return credential;
    }

    // 패키지 내부에서만 호출 가능 — DB 저장 후 생성된 PK를 주입할 때 사용
    public void setId(Long id) { this.id = id; }
}
