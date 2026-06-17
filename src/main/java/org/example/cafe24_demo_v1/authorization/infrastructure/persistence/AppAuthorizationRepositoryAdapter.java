package org.example.cafe24_demo_v1.authorization.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.authorization.domain.model.AppAuthorization;
import org.example.cafe24_demo_v1.authorization.domain.model.AuthorizationId;
import org.example.cafe24_demo_v1.authorization.domain.repository.AppAuthorizationRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 도메인 인터페이스(AppAuthorizationRepository)의 JPA 구현체.
 *
 * 도메인 레이어는 이 클래스의 존재를 모르고, 인터페이스만 의존한다.
 * 이 클래스가 JPA 엔티티 ↔ 도메인 모델 변환과 실제 DB 조작을 모두 처리한다.
 *
 * Spring이 AppAuthorizationRepository 타입의 빈을 요청할 때 이 구현체를 주입한다.
 */
@Repository
@RequiredArgsConstructor
public class AppAuthorizationRepositoryAdapter implements AppAuthorizationRepository {

    private final AppAuthorizationJpaRepository jpaRepository;
    private final AppAuthorizationMapper mapper;

    @Override
    public Optional<AppAuthorization> findById(AuthorizationId id) {
        return jpaRepository.findByMallIdAndClientId(id.mallId(), id.clientId())
                .map(mapper::toDomain); // JPA 엔티티 → 도메인 모델로 변환
    }

    @Override
    public void save(AppAuthorization authorization) {
        AppAuthorizationEntity entity = mapper.toEntity(authorization); // 도메인 → JPA 엔티티
        AppAuthorizationEntity saved = jpaRepository.save(entity);
        authorization.setId(saved.getId()); // DB가 생성한 PK를 도메인 객체에 반영
    }
}
