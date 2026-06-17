package org.example.cafe24_demo_v1.authorization.domain.event;

import lombok.Getter;
import org.example.cafe24_demo_v1.authorization.domain.model.AuthorizationId;

import java.time.LocalDateTime;

/**
 * 액세스 토큰이 갱신됐을 때 발행되는 도메인 이벤트.
 *
 * AppAuthorization.refresh() 호출 시 자동으로 등록된다.
 * 향후 "토큰 갱신 이력 로그" 등의 처리에 활용할 수 있다.
 */
@Getter
public class AuthorizationRefreshed {

    private final AuthorizationId authorizationId; // 갱신된 인가의 식별자
    private final LocalDateTime occurredAt;         // 이벤트 발생 시각

    public AuthorizationRefreshed(AuthorizationId authorizationId) {
        this.authorizationId = authorizationId;
        this.occurredAt = LocalDateTime.now();
    }
}
