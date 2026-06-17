package org.example.cafe24_demo_v1.authorization.application.service;

import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.authorization.application.command.GrantAuthorizationCommand;
import org.example.cafe24_demo_v1.authorization.application.command.RefreshAuthorizationCommand;
import org.example.cafe24_demo_v1.authorization.application.command.RevokeAuthorizationCommand;
import org.example.cafe24_demo_v1.authorization.domain.model.AppAuthorization;
import org.example.cafe24_demo_v1.authorization.domain.model.AuthorizationId;
import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.authorization.domain.repository.AppAuthorizationRepository;
import org.example.cafe24_demo_v1.authorization.domain.service.Cafe24OAuthPort;
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인가(Authorization) 관련 유즈케이스를 조율하는 애플리케이션 서비스.
 *
 * 비즈니스 로직 자체는 도메인 모델(AppAuthorization)에 있고,
 * 이 클래스는 "어떤 순서로 도메인 객체와 외부 포트를 호출할지" 만 결정한다.
 * Controller에서 직접 호출하는 진입점 역할을 한다.
 */
@Service
@RequiredArgsConstructor
public class AppAuthorizationService {

    private final AppAuthorizationRepository repository;
    private final Cafe24OAuthPort oAuthPort;           // Cafe24 외부 API 포트
    private final Cafe24Properties cafe24Properties;   // clientId 등 앱 설정

    /**
     * OAuth 인가 코드를 받아 토큰을 발급하고 인가를 저장한다.
     * 이미 인가가 존재하면 토큰을 갱신하고, 없으면 새로 생성한다 (Upsert).
     *
     * 흐름: 인가 코드 → Cafe24 토큰 발급 → 기존 인가 조회 → 없으면 신규/있으면 갱신 → 저장
     */
    @Transactional
    public void grant(GrantAuthorizationCommand command) {
        // 1. Cafe24 서버에서 토큰 발급
        TokenCredential credential = oAuthPort.issueToken(command.authorizationCode());

        // 2. 인가 식별자 생성 (mallId + clientId)
        AuthorizationId authorizationId = new AuthorizationId(command.mallId(), cafe24Properties.getClientId());

        // 3. 기존 인가가 있으면 토큰 갱신, 없으면 새로 발급
        AppAuthorization authorization = repository.findById(authorizationId)
                .map(existing -> { existing.refresh(credential); return existing; })
                .orElseGet(() -> AppAuthorization.grant(authorizationId, credential));

        repository.save(authorization);
    }

    /**
     * 저장된 리프레시 토큰으로 액세스 토큰을 재발급한다.
     * 인가가 존재하지 않으면 예외를 던진다.
     *
     * 흐름: 인가 조회 → 기존 리프레시 토큰으로 Cafe24 재발급 요청 → 새 자격증명으로 교체 → 저장
     */
    @Transactional
    public void refresh(RefreshAuthorizationCommand command) {
        AuthorizationId authorizationId = new AuthorizationId(command.mallId(), cafe24Properties.getClientId());

        AppAuthorization authorization = repository.findById(authorizationId)
                .orElseThrow(() -> new IllegalStateException("Authorization not found: " + authorizationId));

        // 기존 리프레시 토큰을 사용해 새 자격증명 발급
        TokenCredential newCredential = oAuthPort.refreshToken(
                authorization.getCredential().getRefreshToken()
        );
        authorization.refresh(newCredential);
        repository.save(authorization);
    }

    /**
     * 인가를 취소(REVOKED) 상태로 변경한다.
     * Cafe24 앱 삭제 Webhook 수신 시 WebhookEventService가 호출한다.
     * 인가가 없는 경우 아무 작업도 하지 않는다.
     */
    @Transactional
    public void revoke(RevokeAuthorizationCommand command) {
        AuthorizationId authorizationId = new AuthorizationId(command.mallId(), cafe24Properties.getClientId());

        repository.findById(authorizationId).ifPresent(authorization -> {
            authorization.revoke();
            repository.save(authorization);
        });
    }
}
