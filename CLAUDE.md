# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Cafe24 OAuth 인가(authorization), Webhook 수신, 주문/상품/배송사 동기화, 운영 관측(monitoring/admin)을 구현한 Spring Boot 데모 프로젝트. 기존 Layered Architecture(Controller-Service-Repository)에서 DDD(Domain-Driven Design) 기반 구조로 전환되었고, 이후 추가된 컨텍스트도 같은 구조를 따른다(`README.md` 참고).

## Commands

빌드 시스템은 Gradle. Windows는 `gradlew.bat`, macOS/Linux는 `./gradlew`를 사용한다.

- 빌드: `gradlew.bat build` / `./gradlew build`
- 실행: `gradlew.bat bootRun` / `./gradlew bootRun`
- 전체 테스트: `gradlew.bat test` / `./gradlew test`
- 단일 테스트 클래스: `gradlew.bat test --tests "org.example.cafe24_demo_v1.<패키지>.<클래스명>"` (Unix는 `./gradlew`)
- 단일 테스트 메서드: `gradlew.bat test --tests "org.example.cafe24_demo_v1.<패키지>.<클래스명>.<메서드명>"` (Unix는 `./gradlew`)

신규 기능을 추가할 때는 테스트를 함께 작성한다(`src/test` 참고).

실행 전 다음 환경변수가 필요하다(`src/main/resources/application.yml`, `README.md` 참고): `CAFE24_CLIENT_ID`, `CAFE24_CLIENT_SECRET`, `CAFE24_MALL_ID`, `CAFE24_REDIRECT_URI`, `CAFE24_API_VERSION`(Cafe24 API 호출 시 `X-Cafe24-Api-Version` 헤더 값, 기본값 없음 — 누락 시 주문/상품/배송사 동기화를 포함한 모든 Cafe24 API 호출이 실패), `CAFE24_WEBHOOK_API_KEY`(비어 있으면 모든 Webhook 요청이 거부됨 — fail-closed), `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.

Spring profile은 `local`(기본값, `application-local.yml`, `ddl-auto: update`/`show-sql: true`)과 `prod`(`application-prod.yml`, `ddl-auto: validate`/`show-sql: false`)로 나뉜다. `SPRING_PROFILES_ACTIVE`를 지정하지 않으면 `local`이 적용되므로, 운영 배포 시에는 반드시 `SPRING_PROFILES_ACTIVE=prod`를 설정해야 한다.

Webhook 수신 URL은 기능별로 분리되어 있다(`/webhook/cafe24/app-uninstalled`, `/webhook/cafe24/products/created`, `/webhook/cafe24/products/updated`, `/webhook/cafe24/products/deleted`, `/webhook/cafe24/orders/created`, `/webhook/cafe24/carriers/created`, `/webhook/cafe24/carriers/updated`, `/webhook/cafe24/carriers/deleted`). Cafe24 개발자센터에 각 URL을 해당 이벤트 전용으로 등록한다 — 단일 콜백 URL 환경변수는 없다.

운영용 Backfill/지표 API(`/admin/backfill/*`, `/admin/metrics`)는 인증 없이 호출 가능하다 — 운영 환경에서는 네트워크 레벨로 접근을 제한해야 한다(자세한 내용은 `README.md` 운영 가이드 참고).

## Architecture

패키지는 바운디드 컨텍스트(`authorization`, `order`, `product`, `carrier`, `webhook`, `monitoring`, `admin`, `shared`)로 나뉘고, 각 컨텍스트 내부는 `presentation → application → domain → infrastructure` 4계층 구조를 따른다. `domain` 레이어는 Spring/JPA/HTTP에 의존하지 않는 순수 비즈니스 로직만 가진다. 컨텍스트 성격에 따라 일부 계층은 없을 수 있다 — 예: `admin`은 자체 도메인이 없어 `domain`/`infrastructure`가 없고, `order`는 REST API를 직접 노출하지 않아 `presentation`이 없다. 이는 의도된 생략이며 4계층 외 새 패키지를 추가하는 것과는 다르다(Package Rules 참고).

### authorization 컨텍스트 (OAuth 인가)

- `domain/model/AppAuthorization` — Aggregate Root(`AbstractAggregateRoot` 상속). `grant`/`refresh`/`revoke` 같은 행위 메서드를 통해서만 상태가 바뀌고, 상태 변경 시 도메인 이벤트(`AuthorizationGranted`/`Refreshed`/`Revoked`)를 등록한다. Spring Data가 저장 시 등록된 이벤트를 자동 발행한다.
- `domain/model/AuthorizationId` — mallId + clientId 복합 식별자 Value Object.
- `domain/model/TokenCredential` — 토큰 쌍을 묶은 불변 Value Object. 만료 판단(`isAccessTokenExpired`, `canBeRefreshed`) 로직을 캡슐화한다.
- `domain/repository/AppAuthorizationRepository` — 저장소 포트(인터페이스). 구현체는 `infrastructure/persistence/AppAuthorizationRepositoryAdapter`(JPA 엔티티 ↔ 도메인 변환은 `AppAuthorizationMapper`가 담당).
- `domain/service/Cafe24OAuthPort` — Cafe24 OAuth 외부 API 포트. 구현체는 `infrastructure/external/Cafe24OAuthGateway`. 도메인/애플리케이션 레이어는 이 인터페이스만 알고 실제 HTTP 통신 방식은 모른다.
- `application/service/AppAuthorizationService` — `grant`(Upsert: 없으면 생성, 있으면 갱신)/`refresh`/`revoke` 유즈케이스 조율만 담당. 입력은 `application/command/*Command` 객체로 받는다. 비즈니스 규칙 자체는 `AppAuthorization`에 있다. `getValidCredential(mallId)`는 다른 컨텍스트(order/product/carrier)가 Cafe24 API를 호출하기 전 토큰을 얻는 진입점이며, 액세스 토큰이 만료됐으면 자동으로 refresh한다.
- `presentation/OAuthController` — `/oauth/login`, `/oauth/callback`(state 검증으로 CSRF 방지), `/oauth/refresh`. HTTP 파라미터를 Command로 변환해 서비스에 위임만 한다.

### order 컨텍스트 (주문 동기화 + 주문 Webhook 비동기 재처리)

- `domain/model/Order` — 주문 스냅샷. `applySnapshot`으로만 상태를 갱신한다.
- `domain/model/OrderWebhookEvent` — 주문 Webhook 원본과 재시도 상태(RECEIVED/PROCESSING/PROCESSED/FAILED/DEAD)를 가진 도메인 모델. `markProcessing`/`markProcessed`/`markFailed`/`markDead` 행위 메서드로만 상태가 바뀐다. 재시도 백오프(1분→5분→15분→1시간, 이후 유지)와 PROCESSING stale timeout(5분)을 자체 캡슐화한다.
- `domain/repository/OrderRepository`, `OrderWebhookEventRepository` — 저장소 포트. 구현체는 각각 `infrastructure/persistence/*RepositoryAdapter`.
- `domain/service/Cafe24OrderPort` — Cafe24 주문 API 포트. 구현체는 `infrastructure/external/Cafe24OrderClient`.
- `application/service/OrderService` — `syncFromCafe24`(주기 동기화, "지금 − lookback" 슬라이딩 윈도우)/`backfillFromCafe24`(기간 지정 재동기화)/`upsertFromWebhook`(단건 재조회)을 담당. Cafe24 API 호출 자체가 실패하면 그 실행만 안전하게 중단하고 예외를 스케줄러까지 전파하지 않는다(다음 스케줄이 보완).
- `application/service/OrderWebhookEventService` — `saveRaw`(Webhook 수신 시 원본만 저장, Cafe24 호출 없음)/`processUnprocessed`(재시도 대상 일괄 처리)/`getMetrics`를 담당. Cafe24 응답이 400(Bad Request)인 경우만 영구 오류로 보고 즉시 DEAD, 그 외(401/403/429/5xx 등)는 backoff 재시도.
- `infrastructure/scheduler/OrderSyncScheduler` — `sync.order.fixed-rate-ms` 주기로 `syncFromCafe24` 호출(Webhook 누락 보완용 안전망).
- `infrastructure/scheduler/OrderWebhookEventProcessor` — `worker.order-webhook.fixed-delay-ms` 주기로 `processUnprocessed` 호출.
- `presentation` 없음 — REST API를 직접 노출하지 않고 `webhook`/`admin` 컨텍스트가 서비스를 호출한다.
- 컨텍스트 간 의존은 `order → authorization`, `order → monitoring` 단방향.

### product 컨텍스트 (상품 등록/동기화)

- `domain/model/Product` — 상품 스냅샷. 누락 감지용 `missingSince`/`markMissing`을 가진다.
- `application/service/ProductService` — `register`/`update`/`delete`(API 직접 호출), `upsertFromWebhook`/`deleteFromWebhook`(Webhook 트리거), `syncFromCafe24`(전체 재조회 + `reconcileMissingProducts`로 누락 상품을 2단계로 정리: 1차 누락은 STALE 표시만, 2회 연속 누락이면 로컬 삭제)를 담당.
- `infrastructure/scheduler/ProductSyncScheduler` — `sync.product.cron` 주기로 `syncFromCafe24` 호출.
- `presentation/ProductController` — `/products` CRUD. Cafe24 API 호출 실패는 502(Bad Gateway)로 변환해 응답한다.
- 컨텍스트 간 의존은 `product → authorization`, `product → monitoring` 단방향.

### carrier 컨텍스트 (배송사 등록/동기화)

- `domain/model/Carrier`
- `domain/repository/CarrierWebhookEventRepository` — 배송사 Webhook 중복 수신 이력 포트(자체 테이블 `cafe24_carrier_webhook_event`로 webhook 컨텍스트와 분리돼 있음).
- `application/service/CarrierService` — `registerCarrier`/`syncFromCafe24`(전체 재조회 — Cafe24 배송사 API에 변경시점 필터가 없어 항상 전체를 조회)를 담당.
- `infrastructure/scheduler/CarrierSyncScheduler` — `sync.carrier.cron` 주기로 `syncFromCafe24` 호출.
- `presentation/CarrierController` — `/carriers` 등록.
- 컨텍스트 간 의존은 `carrier → authorization`, `carrier → monitoring` 단방향.

### webhook 컨텍스트

- `presentation/AbstractCafe24WebhookController` — 공통 검증(서명 확인, 필수값 확인)을 담당하는 베이스 클래스. 기능별 컨트롤러(`AppUninstallWebhookController`, `ProductWebhookController`, `OrderWebhookController`, `CarrierWebhookController`)가 상속해 `/webhook/cafe24/*` 엔드포인트를 나눠 처리하고, 검증 통과 시 도메인 이벤트만 발행한다. `x-api-key` 헤더는 `Cafe24WebhookVerifier`로 검증한다(키 미설정 시 fail-closed). Cafe24가 재전송하지 않도록 항상 200을 반환해야 한다.
- `application/WebhookEventService` — `@EventListener`로 구독해 처리한다: 앱 삭제 → `AppAuthorizationService.revoke()`, 상품 생성/수정 → Cafe24 재조회 후 `ProductService.upsertFromWebhook()`, 상품 삭제 → `ProductService.deleteFromWebhook()`, 배송사 생성/수정/삭제 → `CarrierWebhookEventRepository`에 이력만 기록(실제 반영은 `CarrierSyncScheduler` 주기 동기화가 전담). 주문 생성은 직접 처리하지 않고 `OrderWebhookEventService.saveRaw()`로 위임만 한다(order 컨텍스트가 비동기로 전담).
- 중복 수신은 `(event_no, mall_id, resource_id)` 조합으로 막는다. 앱삭제/상품 이력은 `WebhookEventRepository`(webhook 컨텍스트 소유, 테이블 `cafe24_product_webhook_event`), 배송사 이력은 `CarrierWebhookEventRepository`(carrier 컨텍스트 소유), 주문 이력은 `OrderWebhookEventRepository`(order 컨텍스트 소유)로 컨텍스트별로 나뉘어 있다.
- 컨텍스트 간 의존은 `webhook → authorization`, `webhook → product`, `webhook → order`, `webhook → carrier` 단방향이며, Spring 도메인 이벤트(`@EventListener`)로 느슨하게 연결된다.

### monitoring 컨텍스트 (동기화 운영 지표)

- `domain/model/SyncRunStatus` — mallId + `SyncTarget`(ORDER/PRODUCT/CARRIER) 단위로 가장 최근 동기화 실행 결과 1건만 upsert 저장한다(이력 누적 아님). Cafe24 API 호출이 끝까지 성공했을 때(`apiFailureCount == 0`)만 `lastSuccessAt`을 갱신한다.
- `application/service/SyncMetricsService` — `recordRun`(동기화 종료 시 결과 기록)/`getAll`(mallId 기준 조회)을 담당.
- 컨텍스트 간 의존은 `order/product/carrier → monitoring` 단방향이며, authorization 참조 패턴과 동일하다. monitoring은 다른 컨텍스트를 알지 못한다.

### admin 컨텍스트 (운영용 Backfill/지표 API)

- 자체 도메인 모델이 없다 — `BackfillService`/`MetricsService`는 order/product/carrier/monitoring 컨텍스트의 기존 서비스를 호출해 결과를 조합만 한다.
- `presentation/AdminBackfillController` — `/admin/backfill/orders|products|carriers`. `presentation/AdminMetricsController` — `/admin/metrics`.
- **인증 미들웨어가 없다.** 운영 환경에서는 네트워크 레벨(방화벽, 리버스 프록시 IP 화이트리스트 등)로 접근을 제한해야 한다.

### 컨텍스트 간 흐름 예시 (앱 삭제)

```
Webhook 수신 → AppUninstallWebhookController가 AppUninstalledEvent 발행
  → WebhookEventService(@EventListener)가 중복 체크 후 처리
  → AppAuthorizationService.revoke() → AppAuthorization.revoke()
  → AuthorizationRevoked 이벤트 등록 (Aggregate 저장 시 함께 발행)
```

### 컨텍스트 간 흐름 예시 (주문 Webhook → 비동기 반영)

```
Webhook 수신 → OrderWebhookController가 OrderCreatedEvent 발행 (Cafe24 호출 없이 즉시 200 응답)
  → WebhookEventService(@EventListener) → OrderWebhookEventService.saveRaw()
  → cafe24_order_webhook_event에 RECEIVED로 저장하고 끝
  ⋯ (별도 주기, OrderWebhookEventProcessor) ⋯
  → OrderWebhookEventService.processUnprocessed() → Cafe24 주문 재조회
  → OrderService.upsertFromWebhook() → order 테이블 반영
  → 성공: PROCESSED / 일시 오류: FAILED(백오프 재시도) / 영구 오류(HTTP 400): DEAD
```

새 컨텍스트나 유즈케이스를 추가할 때도 이 패턴(포트/어댑터로 외부 의존성 분리, Command 객체로 입력 전달, 도메인 이벤트로 부수효과 전파, Aggregate가 스스로 상태/이벤트를 관리)을 따른다. Webhook을 신뢰할 수 없는 트리거로만 취급하고 별도 주기 동기화로 보완하는 패턴(order/product/carrier)도 새 리소스를 추가할 때 기본으로 따른다.

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

위 4계층 외 새로운 최상위 패키지를 생성하지 않는다. 다만 컨텍스트에 필요 없는 계층은 생략할 수 있다(예: 자체 도메인이 없는 `admin`은 domain/infrastructure 없이 application/presentation만 가짐, REST API가 없는 `order`는 presentation 없음) — 이는 4계층 외 새 패키지를 추가하는 것과는 다르다.

## webhook Rules
- 
- Cafe24 Webhook은 누락될 수 있다고 가정한다.
- Webhook은 Trigger 역할만 수행한다.

## Order Rules

- 주문 데이터의 Source of Truth는 Cafe24 Orders API이다.
- Webhook 데이터만으로 주문 상태를 결정하지 않는다.
- 모든 주문 동기화는 멱등성을 보장해야 한다.

## Tech Stack

- Java 21
- Spring Boot 3.5.x
- Spring Data JPA
- MySQL 8
- Gradle