# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Cafe24 OAuth 인가(authorization) 흐름과 Webhook 수신을 구현한 Spring Boot 데모 프로젝트. 기존 Layered Architecture(Controller-Service-Repository)에서 DDD(Domain-Driven Design) 기반 구조로 전환되었다(`README.md` 참고).

## Commands

빌드 시스템은 Gradle. 이 저장소에는 `gradlew.bat`(Windows)만 있고 유닉스용 `gradlew`는 없다.

- 빌드: `gradlew.bat build`
- 실행: `gradlew.bat bootRun`
- 전체 테스트: `gradlew.bat test`
- 단일 테스트 클래스: `gradlew.bat test --tests "org.example.cafe24_demo_v1.<패키지>.<클래스명>"`
- 단일 테스트 메서드: `gradlew.bat test --tests "org.example.cafe24_demo_v1.<패키지>.<클래스명>.<메서드명>"`

`src/test`에는 아직 테스트가 하나도 없다 — 신규 기능을 추가할 때 테스트를 새로 작성해야 한다.

실행 전 다음 환경변수가 필요하다(`src/main/resources/application.yml`, `README.md` 참고): `CAFE24_CLIENT_ID`, `CAFE24_CLIENT_SECRET`, `CAFE24_MALL_ID`, `CAFE24_REDIRECT_URI`, `CAFE24_WEBHOOK_API_KEY`(로컬 개발 시 생략 가능), `CAFE24_WEBHOOK_CALLBACK_URL`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.

## Architecture

패키지는 바운디드 컨텍스트(`authorization`, `webhook`, `shared`)로 나뉘고, 각 컨텍스트 내부는 `presentation → application → domain → infrastructure` 4계층 구조를 따른다. `domain` 레이어는 Spring/JPA/HTTP에 의존하지 않는 순수 비즈니스 로직만 가진다.

### authorization 컨텍스트 (OAuth 인가)

- `domain/model/AppAuthorization` — Aggregate Root(`AbstractAggregateRoot` 상속). `grant`/`refresh`/`revoke` 같은 행위 메서드를 통해서만 상태가 바뀌고, 상태 변경 시 도메인 이벤트(`AuthorizationGranted`/`Refreshed`/`Revoked`)를 등록한다. Spring Data가 저장 시 등록된 이벤트를 자동 발행한다.
- `domain/model/AuthorizationId` — mallId + clientId 복합 식별자 Value Object.
- `domain/model/TokenCredential` — 토큰 쌍을 묶은 불변 Value Object. 만료 판단(`isAccessTokenExpired`, `canBeRefreshed`) 로직을 캡슐화한다.
- `domain/repository/AppAuthorizationRepository` — 저장소 포트(인터페이스). 구현체는 `infrastructure/persistence/AppAuthorizationRepositoryAdapter`(JPA 엔티티 ↔ 도메인 변환은 `AppAuthorizationMapper`가 담당).
- `domain/service/Cafe24OAuthPort` — Cafe24 OAuth 외부 API 포트. 구현체는 `infrastructure/external/Cafe24OAuthGateway`. 도메인/애플리케이션 레이어는 이 인터페이스만 알고 실제 HTTP 통신 방식은 모른다.
- `application/service/AppAuthorizationService` — `grant`(Upsert: 없으면 생성, 있으면 갱신)/`refresh`/`revoke` 유즈케이스 조율만 담당. 입력은 `application/command/*Command` 객체로 받는다. 비즈니스 규칙 자체는 `AppAuthorization`에 있다.
- `presentation/OAuthController` — `/oauth/login`, `/oauth/callback`(state 검증으로 CSRF 방지), `/oauth/refresh`. HTTP 파라미터를 Command로 변환해 서비스에 위임만 한다.

### webhook 컨텍스트

- `presentation/WebhookController` — `/webhook/cafe24`. `x-api-key` 헤더를 `Cafe24WebhookVerifier`로 검증한 뒤, `event_no`에 따라 `ApplicationEventPublisher`로 도메인 이벤트만 발행한다(직접 비즈니스 처리는 하지 않음). Cafe24가 재전송하지 않도록 항상 200을 반환해야 한다.
- `application/WebhookEventService` — `@EventListener`로 `AppUninstalledEvent`를 구독해 `AppAuthorizationService.revoke()`를 호출한다. `WebhookEventRepository`로 (eventNo, mallId) 조합의 중복 수신 여부를 확인해 멱등성을 보장한다(Cafe24가 동일 이벤트를 여러 번 전송할 수 있음).
- 컨텍스트 간 의존은 `webhook → authorization` 단방향이며, Spring 도메인 이벤트(`@EventListener`)로 느슨하게 연결된다.

### 컨텍스트 간 흐름 예시 (앱 삭제)

```
Webhook 수신 → WebhookController가 AppUninstalledEvent 발행
  → WebhookEventService(@EventListener)가 중복 체크 후 처리
  → AppAuthorizationService.revoke() → AppAuthorization.revoke()
  → AuthorizationRevoked 이벤트 등록 (Aggregate 저장 시 함께 발행)
```

새 컨텍스트나 유즈케이스를 추가할 때도 이 패턴(포트/어댑터로 외부 의존성 분리, Command 객체로 입력 전달, 도메인 이벤트로 부수효과 전파, Aggregate가 스스로 상태/이벤트를 관리)을 따른다.

## Code Style

- 생성자 주입 사용, 필드 주입 금지
- Lombok 최소 사용, Optional 남용 금지, 가독성 우선
- Controller는 요청/응답 처리만, 비즈니스 로직은 Service, Repository는 데이터 접근만 담당

## Testing

- JUnit5 + AssertJ 사용
- 신규 기능은 테스트 필수
- 테스트 실패 시 원인 분석 후 수정 (현재 테스트가 전혀 없으므로 새 기능 작업 시 처음 작성하게 될 가능성이 높다)

## Workflow Rules

- 코드 수정 전: (1) 문제 원인 분석 (2) 수정 계획 (3) 변경 대상 파일 목록을 먼저 설명하고, 승인을 받은 후 수정한다.
- 코드 변경 시: 관련 테스트 작성 → 기존 테스트 실행 → 결과 보고.
- 한 번에 5개 이상 파일을 수정해야 하면 먼저 변경 계획을 설명하고 승인을 받는다.
- `git reset --hard`, `git clean -fd` 실행 금지. 승인 없이 커밋 금지.

## Package Rules

새로운 기능 추가 시 기존 패키지 구조를 따른다.

presentation
application
domain
infrastructure

위 4계층 외 새로운 최상위 패키지를 생성하지 않는다.

## Tech Stack

- Java 21
- Spring Boot 3.5.x
- Spring Data JPA
- MySQL 8
- Gradle