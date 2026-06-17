package org.example.cafe24_demo_v1.authorization.application.command;

/**
 * 토큰 갱신 유즈케이스에 전달하는 커맨드 객체.
 *
 * @param mallId 갱신할 인가의 쇼핑몰 ID
 */
public record RefreshAuthorizationCommand(String mallId) {}
