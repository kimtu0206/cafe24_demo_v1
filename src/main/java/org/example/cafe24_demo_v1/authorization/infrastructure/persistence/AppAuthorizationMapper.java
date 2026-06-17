package org.example.cafe24_demo_v1.authorization.infrastructure.persistence;

import org.example.cafe24_demo_v1.authorization.domain.model.AppAuthorization;
import org.example.cafe24_demo_v1.authorization.domain.model.AuthorizationId;
import org.example.cafe24_demo_v1.authorization.domain.model.AuthorizationStatus;
import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.springframework.stereotype.Component;

/**
 * 도메인 모델(AppAuthorization) ↔ JPA 엔티티(AppAuthorizationEntity) 간 변환을 담당하는 매퍼.
 *
 * 도메인 모델과 JPA 엔티티를 분리하면 각각 독립적으로 변경할 수 있다.
 * 예를 들어 DB 컬럼을 추가해도 도메인 모델을 수정하지 않아도 되고,
 * 도메인 로직이 바뀌어도 DB 스키마를 건드리지 않아도 된다.
 */
@Component
class AppAuthorizationMapper {

    /** JPA 엔티티 → 도메인 모델 변환 (DB에서 조회 후 도메인으로 복원할 때 사용) */
    AppAuthorization toDomain(AppAuthorizationEntity entity) {
        return AppAuthorization.reconstitute(
                entity.getId(),
                new AuthorizationId(entity.getMallId(), entity.getClientId()),
                new TokenCredential(
                        entity.getAccessToken(),
                        entity.getRefreshToken(),
                        entity.getTokenType(),
                        entity.getAccessTokenExpiresAt(),
                        entity.getRefreshTokenExpiresAt()
                ),
                // 기존 DB 레코드에 status 컬럼이 없을 경우(null) ACTIVE로 취급
                entity.getStatus() != null
                        ? AuthorizationStatus.valueOf(entity.getStatus())
                        : AuthorizationStatus.ACTIVE,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /** 도메인 모델 → JPA 엔티티 변환 (저장할 때 사용) */
    AppAuthorizationEntity toEntity(AppAuthorization domain) {
        AppAuthorizationEntity entity = new AppAuthorizationEntity();
        entity.setId(domain.getId());
        entity.setMallId(domain.getAuthorizationId().mallId());
        entity.setClientId(domain.getAuthorizationId().clientId());
        entity.setAccessToken(domain.getCredential().getAccessToken());
        entity.setRefreshToken(domain.getCredential().getRefreshToken());
        entity.setTokenType(domain.getCredential().getTokenType());
        entity.setAccessTokenExpiresAt(domain.getCredential().getAccessTokenExpiresAt());
        entity.setRefreshTokenExpiresAt(domain.getCredential().getRefreshTokenExpiresAt());
        entity.setStatus(domain.getStatus().name());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
