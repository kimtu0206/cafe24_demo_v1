package org.example.cafe24_demo_v1.authorization.domain.service;

import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;

/**
 * Cafe24 OAuth 서버와 통신하는 외부 포트(Port) 인터페이스.
 *
 * 도메인 레이어에 위치하지만 구현체는 infrastructure 레이어의 Cafe24OAuthGateway가 담당한다.
 * 덕분에 도메인 코드는 "HTTP 통신을 어떻게 하는지" 를 전혀 몰라도 된다.
 * (Hexagonal Architecture의 Port & Adapter 패턴)
 */
public interface Cafe24OAuthPort {

    /**
     * 인가 코드(Authorization Code)를 Cafe24 서버에 전달해 액세스/리프레시 토큰을 발급받는다.
     * OAuth 2.0의 Authorization Code Grant 방식.
     */
    TokenCredential issueToken(String authorizationCode);

    /**
     * 리프레시 토큰으로 만료된 액세스 토큰을 재발급받는다.
     * OAuth 2.0의 Refresh Token Grant 방식.
     */
    TokenCredential refreshToken(String refreshToken);
}
