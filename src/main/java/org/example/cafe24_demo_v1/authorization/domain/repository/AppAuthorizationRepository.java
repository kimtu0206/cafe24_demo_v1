package org.example.cafe24_demo_v1.authorization.domain.repository;

import org.example.cafe24_demo_v1.authorization.domain.model.AppAuthorization;
import org.example.cafe24_demo_v1.authorization.domain.model.AuthorizationId;

import java.util.Optional;

/**
 * 인가(AppAuthorization) 저장소 인터페이스.
 *
 * 도메인 레이어에 위치하기 때문에 JPA나 DB에 대한 의존성이 전혀 없다.
 * 실제 구현체(JPA)는 infrastructure 레이어의 AppAuthorizationRepositoryAdapter가 담당한다.
 *
 * 이렇게 인터페이스를 도메인에 두면, 나중에 DB를 바꾸거나 테스트용 가짜 구현체로 교체해도
 * 도메인 코드를 전혀 수정하지 않아도 된다.
 */
public interface AppAuthorizationRepository {

    /** mallId + clientId 조합으로 인가를 조회한다. 없으면 빈 Optional 반환. */
    Optional<AppAuthorization> findById(AuthorizationId id);

    /**
     * 인가를 저장하거나 업데이트한다. 저장된 인가를 반환한다.
     */
    void save(AppAuthorization authorization);
}
