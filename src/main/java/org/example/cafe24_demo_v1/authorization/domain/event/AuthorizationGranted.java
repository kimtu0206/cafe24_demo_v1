package org.example.cafe24_demo_v1.authorization.domain.event;

import lombok.Getter;
import org.example.cafe24_demo_v1.authorization.domain.model.AuthorizationId;

import java.time.LocalDateTime;

/**
 * 인가가 최초로 발급됐을 때 발행되는 도메인 이벤트.
 *
 * AppAuthorization.grant() 호출 시 자동으로 등록된다.
 * 현재는 별도 구독자가 없지만, 향후 "앱 설치 완료 알림" 등의 처리에 활용할 수 있다.
 */
@Getter
public class AuthorizationGranted {

    private final AuthorizationId authorizationId; // 발급된 인가의 식별자
    private final LocalDateTime occurredAt;         // 이벤트 발생 시각

    public AuthorizationGranted(AuthorizationId authorizationId) {
        this.authorizationId = authorizationId;
        this.occurredAt = LocalDateTime.now();
    }
}
