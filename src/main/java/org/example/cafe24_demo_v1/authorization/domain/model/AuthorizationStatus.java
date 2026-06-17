package org.example.cafe24_demo_v1.authorization.domain.model;

/**
 * 인가의 현재 상태를 나타내는 열거형.
 *
 * ACTIVE  : 정상적으로 사용 가능한 상태
 * REVOKED : 앱 삭제 등으로 취소된 상태 (재활성화 불가)
 */
public enum AuthorizationStatus {
    ACTIVE,
    REVOKED
}
