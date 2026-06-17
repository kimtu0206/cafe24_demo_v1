package org.example.cafe24_demo_v1.authorization.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * JPA 기반 DB 접근 인터페이스.
 *
 * Spring Data JPA가 구현체를 자동 생성한다.
 * 도메인 레이어에 노출되지 않도록 패키지 기본 접근자(package-private)로 선언한다.
 * 외부에서는 AppAuthorizationRepository(도메인 인터페이스)를 통해서만 접근한다.
 */
interface AppAuthorizationJpaRepository extends JpaRepository<AppAuthorizationEntity, Long> {

    /** mallId + clientId 조합으로 인가 엔티티를 조회한다. */
    Optional<AppAuthorizationEntity> findByMallIdAndClientId(String mallId, String clientId);
}
