package org.example.cafe24_demo_v1.authorization.application.command;

/**
 *
 *
 *
 * 인가 취소 유즈케이스에 전달하는 커맨드 객체.
 * 주로 앱 삭제 Webhook 수신 시 WebhookEventService가 생성한다.
 *
 * @param mallId 취소할 인가의 쇼핑몰 ID
 */
public record RevokeAuthorizationCommand(String mallId) {}
