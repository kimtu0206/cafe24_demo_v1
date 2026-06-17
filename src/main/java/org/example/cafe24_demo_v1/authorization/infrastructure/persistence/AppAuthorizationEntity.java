package org.example.cafe24_demo_v1.authorization.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DB 테이블(cafe24_token)과 매핑되는 JPA 엔티티.
 *
 * 도메인 모델(AppAuthorization)과 분리된 별도 클래스이다.
 * 도메인 모델은 JPA 어노테이션을 전혀 모르고, 이 클래스가 JPA 영속화를 전담한다.
 * 외부에서 직접 사용하지 않도록 패키지 기본 접근자(package-private)로 선언한다.
 */
@Entity
@Table(name = "cafe24_token")
@Getter
@Setter
class AppAuthorizationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String mallId;    // 쇼핑몰 ID
    private String clientId;  // OAuth 앱 클라이언트 ID

    @Column(length = 1000)
    private String accessToken;  // 액세스 토큰 (길이가 길어 1000자로 설정)

    @Column(length = 1000)
    private String refreshToken; // 리프레시 토큰

    private String tokenType;                    // 토큰 타입 (보통 "Bearer")
    private LocalDateTime accessTokenExpiresAt;  // 액세스 토큰 만료 시각
    private LocalDateTime refreshTokenExpiresAt; // 리프레시 토큰 만료 시각
    private String status;                       // 인가 상태 (ACTIVE / REVOKED)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
