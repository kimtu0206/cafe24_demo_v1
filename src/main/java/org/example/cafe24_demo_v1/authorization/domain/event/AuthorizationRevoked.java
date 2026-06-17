package org.example.cafe24_demo_v1.authorization.domain.event;

import lombok.Getter;
import org.example.cafe24_demo_v1.authorization.domain.model.AuthorizationId;

import java.time.LocalDateTime;

/**
 * 인가가 취소됐을 때 발행되는 도메인 이벤트.
 *
 * AppAuthorization.revoke() 호출 시 자동으로 등록된다.
 * 앱 삭제 Webhook 수신 → WebhookEventService → revoke() → 이 이벤트 발행 순서로 동작한다.
 * 향후 "앱 삭제 후 데이터 정리" 등의 처리를 이 이벤트를 구독해 처리할 수 있다.
 */
@Getter
public class AuthorizationRevoked {

    private final AuthorizationId authorizationId; // 취소된 인가의 식별자
    private final LocalDateTime occurredAt;         // 이벤트 발생 시각

    public AuthorizationRevoked(AuthorizationId authorizationId) {
        this.authorizationId = authorizationId;
        this.occurredAt = LocalDateTime.now();
    }
}
