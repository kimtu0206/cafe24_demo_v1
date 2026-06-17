package org.example.cafe24_demo_v1.authorization.application.command;

/**
 * 인가 발급 유즈케이스에 전달하는 커맨드 객체.
 *
 * Controller가 HTTP 파라미터를 이 객체로 변환해 AppAuthorizationService에 전달한다.
 * record를 사용해 불변 값 객체로 만든다.
 *
 * @param mallId            Cafe24 쇼핑몰 ID
 * @param authorizationCode OAuth 인가 서버로부터 받은 일회용 코드
 */
public record GrantAuthorizationCommand(String mallId, String authorizationCode) {}
