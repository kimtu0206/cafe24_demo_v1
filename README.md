# Cafe24 Demo v1

Cafe24 OAuth 인가, Webhook 수신, 주문/상품/배송사 동기화를 구현한 Spring Boot 프로젝트입니다.  
기존 Layered Architecture(Controller-Service-Repository)에서 **DDD(Domain-Driven Design) 기반 구조**로 전환했고, 이후 추가된 order/product/carrier/monitoring/admin 컨텍스트도 같은 구조(`presentation → application → domain → infrastructure`)를 따릅니다.

---

## 기술 스택

- Java 21
- Spring Boot 3.5.3
- Spring Data JPA
- MySQL
- Lombok

---

## 환경변수

애플리케이션 실행 전 아래 환경변수를 설정해야 합니다.

| 변수명 | 설명 |
|---|---|
| `CAFE24_CLIENT_ID` | Cafe24 앱 클라이언트 ID |
| `CAFE24_CLIENT_SECRET` | Cafe24 앱 클라이언트 Secret |
| `CAFE24_MALL_ID` | 연동할 쇼핑몰 ID |
| `CAFE24_REDIRECT_URI` | OAuth 콜백 URI |
| `CAFE24_API_VERSION` | Cafe24 API 호출 시 `X-Cafe24-Api-Version` 헤더 값(예: `2025-09-01`). 기본값이 없어 누락 시 주문/상품/배송사 동기화를 포함한 모든 Cafe24 API 호출이 실패함 |
| `CAFE24_WEBHOOK_API_KEY` | Webhook 서명 검증 키 (비어 있으면 모든 Webhook 요청이 거부됨) |
| `DB_URL` | MySQL 접속 URL |
| `DB_USERNAME` | DB 사용자명 |
| `DB_PASSWORD` | DB 비밀번호 |

---

## Profile 설정

`SPRING_PROFILES_ACTIVE`를 지정하지 않으면 기본값으로 `local` profile이 적용된다.

| Profile | 파일 | ddl-auto | show-sql | 용도 |
|---|---|---|---|---|
| `local` (기본값) | `application-local.yml` | `update` | `true` | 로컬 개발 — 엔티티 수정 시 DB 스키마 자동 반영 |
| `prod` | `application-prod.yml` | `validate` | `false` | 운영 — 스키마는 변경하지 않고 엔티티와 DB 구조만 검증, SQL 로그 비노출 |

운영 환경에 배포할 때는 `SPRING_PROFILES_ACTIVE=prod` 환경변수를 반드시 설정해야 한다.

---

## API 엔드포인트

### OAuth 인가

| 메서드 | URL | 설명 |
|---|---|---|
| `GET` | `/oauth/login` | OAuth 로그인 시작 (Cafe24 인가 서버로 리다이렉트) |
| `GET` | `/oauth/callback` | OAuth 콜백 수신 및 토큰 저장 |
| `POST` | `/oauth/refresh` | 액세스 토큰 수동 갱신 |

### Webhook 수신 (Cafe24 → 서버)

| 메서드 | URL | 설명 |
|---|---|---|
| `POST` | `/webhook/cafe24/app-uninstalled` | 앱 삭제 Webhook 수신 → 인가 즉시 REVOKED |
| `POST` | `/webhook/cafe24/products/created` | 상품 생성 Webhook 수신 → Cafe24 재조회 후 로컬 upsert |
| `POST` | `/webhook/cafe24/products/updated` | 상품 수정 Webhook 수신 → Cafe24 재조회 후 로컬 upsert |
| `POST` | `/webhook/cafe24/products/deleted` | 상품 삭제 Webhook 수신 → 로컬 DB에서만 삭제 |
| `POST` | `/webhook/cafe24/orders/created` | 주문 생성 Webhook 수신 → 원본만 저장(비동기 반영, 운영 가이드 1·3번 참고) |
| `POST` | `/webhook/cafe24/carriers/created` | 배송사 등록 Webhook 수신 → 이력만 기록(반영은 주기 동기화가 전담) |
| `POST` | `/webhook/cafe24/carriers/updated` | 배송사 수정 Webhook 수신 → 이력만 기록 |
| `POST` | `/webhook/cafe24/carriers/deleted` | 배송사 삭제 Webhook 수신 → 이력만 기록 |

### 상품 / 배송사 관리

| 메서드 | URL | 설명 |
|---|---|---|
| `GET` | `/products` | 로컬 DB에 저장된 상품 목록 조회(페이지네이션, Cafe24 호출 없음) |
| `POST` | `/products` | Cafe24에 신규 상품 등록 |
| `PUT` | `/products/{productNo}` | Cafe24 상품 수정 |
| `DELETE` | `/products/{productNo}` | Cafe24 상품 삭제 |
| `POST` | `/carriers` | Cafe24에 배송사 등록 |

### 운영(Admin) — ⚠️ 인증 없음 (운영 가이드 9번 참고)

| 메서드 | URL | 설명 |
|---|---|---|
| `POST` | `/admin/backfill/orders?startDate=&endDate=` | 지정 기간 주문 재동기화 |
| `POST` | `/admin/backfill/products` | 상품 전체 재동기화 |
| `POST` | `/admin/backfill/carriers` | 배송사 전체 재동기화 |
| `GET` | `/admin/metrics` | Webhook 처리 현황 + 동기화 실행 현황 조회 |

---

## 패키지 구조

바운디드 컨텍스트는 총 7개다(`authorization`/`order`/`product`/`carrier`/`webhook`/`monitoring`/`admin` + 공용 `shared`). `authorization`은 최초 DDD 전환 당시의 기준 예시라 파일 단위까지 펼쳐뒀고, 나머지는 같은 4계층 패턴을 따르므로 계층별 핵심 파일만 정리했다.

```
src/main/java/org/example/cafe24_demo_v1/
│
├── authorization/                          # OAuth 인가 컨텍스트 (DDD 패턴의 기준 예시)
│   ├── domain/                             # 핵심 비즈니스 로직 (외부 의존성 없음)
│   │   ├── model/
│   │   │   ├── AppAuthorization.java       # Aggregate Root
│   │   │   ├── AuthorizationId.java        # Value Object (mallId + clientId)
│   │   │   ├── TokenCredential.java        # Value Object (토큰 쌍, 불변)
│   │   │   └── AuthorizationStatus.java    # Enum (ACTIVE / REVOKED)
│   │   ├── event/
│   │   │   ├── AuthorizationGranted.java   # 인가 발급 이벤트
│   │   │   ├── AuthorizationRefreshed.java # 토큰 갱신 이벤트
│   │   │   └── AuthorizationRevoked.java   # 인가 취소 이벤트
│   │   ├── repository/
│   │   │   └── AppAuthorizationRepository.java  # 저장소 인터페이스
│   │   └── service/
│   │       └── Cafe24OAuthPort.java             # 외부 OAuth 포트 인터페이스
│   │
│   ├── application/                        # 유즈케이스 조율
│   │   ├── command/
│   │   │   ├── GrantAuthorizationCommand.java
│   │   │   ├── RefreshAuthorizationCommand.java
│   │   │   └── RevokeAuthorizationCommand.java
│   │   └── service/
│   │       └── AppAuthorizationService.java       # 다른 컨텍스트도 getValidCredential()로 의존
│   │
│   ├── infrastructure/                     # 기술적 구현 (DB, HTTP)
│   │   ├── external/
│   │   │   ├── Cafe24OAuthGateway.java     # Cafe24 OAuth API 어댑터
│   │   │   └── Cafe24TokenResponse.java    # 외부 API 응답 DTO
│   │   └── persistence/
│   │       ├── AppAuthorizationEntity.java          # JPA 엔티티 (테이블: cafe24_token)
│   │       ├── AppAuthorizationJpaRepository.java   # Spring Data JPA 인터페이스
│   │       ├── AppAuthorizationMapper.java          # 도메인 ↔ JPA 변환
│   │       └── AppAuthorizationRepositoryAdapter.java # 저장소 구현체
│   │
│   └── presentation/                       # HTTP 입출력
│       └── OAuthController.java
│
├── order/                                  # 주문 동기화 + 주문 Webhook 비동기 재처리
│   ├── domain/
│   │   ├── model/         Order.java · OrderWebhookEvent.java(재시도 상태 머신) · OrderWebhookEventStatus.java
│   │   ├── repository/    OrderRepository.java · OrderWebhookEventRepository.java (포트)
│   │   └── service/       Cafe24OrderPort.java (포트)
│   ├── application/service/  OrderService.java(동기화/Backfill) · OrderWebhookEventService.java(재시도 처리)
│   └── infrastructure/
│       ├── external/      Cafe24OrderClient.java
│       ├── persistence/   Entity(테이블: cafe24_order, cafe24_order_webhook_event) · Mapper · RepositoryAdapter
│       └── scheduler/     OrderSyncScheduler.java · OrderWebhookEventProcessor.java
│   # presentation 없음 — REST API를 직접 노출하지 않고 webhook/admin 컨텍스트가 호출만 한다
│
├── product/                                # 상품 등록/동기화
│   ├── domain/model/      Product.java · ProductPage.java · ProductRegistration.java
│   ├── application/       command/(Create·Update·DeleteProductCommand) · service/ProductService.java
│   ├── infrastructure/
│   │   ├── external/      Cafe24ProductClient.java
│   │   ├── persistence/   Entity(테이블: cafe24_product) · Mapper · RepositoryAdapter
│   │   └── scheduler/     ProductSyncScheduler.java
│   └── presentation/      ProductController.java (`/products`)
│
├── carrier/                                # 배송사 등록/동기화 + 배송사 Webhook 이력
│   ├── domain/
│   │   ├── model/         Carrier.java
│   │   └── repository/    CarrierRepository.java · CarrierWebhookEventRepository.java (포트)
│   ├── application/       command/RegisterCarrierCommand.java · service/CarrierService.java
│   ├── infrastructure/
│   │   ├── external/      Cafe24CarrierClient.java
│   │   ├── persistence/   Entity(테이블: cafe24_carrier, cafe24_carrier_webhook_event) · Mapper · RepositoryAdapter
│   │   └── scheduler/     CarrierSyncScheduler.java
│   └── presentation/      CarrierController.java (`/carriers`)
│
├── webhook/                                # Cafe24 Webhook 수신 공통 처리 + 이벤트 발행
│   ├── domain/
│   │   ├── event/         App/Order/Product/Carrier 각 *CreatedEvent 등
│   │   ├── model/         WebhookEventType.java
│   │   └── repository/    WebhookEventRepository.java (포트 — 앱삭제·상품 이력 전용)
│   ├── application/       WebhookEventService.java  # 이벤트 구독 → 중복 체크 → 각 컨텍스트 서비스 호출
│   ├── infrastructure/    Cafe24WebhookVerifier.java · persistence/(테이블: cafe24_product_webhook_event)
│   └── presentation/
│       ├── AbstractCafe24WebhookController.java     # 서명/필수값 검증 공통 로직
│       ├── AppUninstallWebhookController.java
│       ├── ProductWebhookController.java
│       ├── OrderWebhookController.java
│       └── CarrierWebhookController.java
│
├── monitoring/                             # 동기화 실행 현황 기록 (order/product/carrier → monitoring 단방향 의존)
│   ├── domain/model/      SyncRunStatus.java · SyncTarget.java
│   ├── application/       service/SyncMetricsService.java
│   └── infrastructure/    persistence/(테이블: sync_run_status)
│
├── admin/                                  # 운영용 Backfill/지표 API (자체 도메인 모델 없음, 인증 없음)
│   ├── application/       BackfillService.java · MetricsService.java
│   └── presentation/      AdminBackfillController.java · AdminMetricsController.java
│
└── shared/                                 # 공용 설정/예외
    ├── config/             Cafe24Properties.java · WorkerProperties.java · SyncProperties.java · AppConfig.java
    └── exception/          Cafe24ApiException.java
```

---

## DDD 전환 내용

> 아래는 최초 DDD 전환(인가/Webhook) 당시의 매핑이다. 이후 추가된 order/product/carrier/monitoring/admin 컨텍스트도 동일한 패턴(포트/어댑터, Command 객체로 입력 전달, 도메인 이벤트로 부수효과 전파)을 그대로 따른다 — 패키지 구조는 위 트리, 실제 동작 방식은 아래 "운영 가이드"를 참고.

### 기존 구조 (Layered Architecture)

```
controller/
  OAuthController.java
  WebhookController.java
service/
  OAuthService.java       ← HTTP 통신 + 도메인 로직 혼재
  TokenService.java       ← 날짜 파싱 + 저장 + 갱신 모두 담당
repository/
  TokenRepository.java    ← JPA를 도메인에 직접 노출
entity/
  Token.java              ← @Getter @Setter만 있는 빈 껍데기
dto/
  TokenResponse.java
  WebhookRequest.java
```

### 변경 후 구조 (DDD)

기존 파일들이 아래와 같이 역할에 따라 분리됐습니다.

| 기존 | 변경 후 | 이유 |
|---|---|---|
| `Token.java` | `AppAuthorization.java` | 행위 메서드(`grant`, `refresh`, `revoke`, `isExpired`) 추가 |
| `String mallId, clientId` (낱개 필드) | `AuthorizationId.java` | mallId + clientId를 하나의 식별자로 묶음 |
| `String accessToken, refreshToken` (낱개 필드) | `TokenCredential.java` | 토큰 쌍을 불변 객체로 묶고 만료 판단 로직 포함 |
| `OAuthService.java` | `Cafe24OAuthGateway.java` | HTTP 통신만 담당하도록 인프라 레이어로 분리 |
| `TokenService.java` | `AppAuthorizationService.java` | 순수 유즈케이스 조율만 담당 |
| `TokenRepository.java` | `AppAuthorizationRepository.java` (인터페이스) + `AppAuthorizationRepositoryAdapter.java` (구현체) | 도메인이 JPA에 직접 의존하지 않도록 분리 |
| `WebhookController.java` (println 스텁) | `WebhookController` + `WebhookEventService` + `Cafe24WebhookVerifier` | 서명 검증, 도메인 이벤트 발행, 인가 취소 처리 연동 |

---

### 핵심 개념

#### 1. Aggregate Root — `AppAuthorization`

인가의 생명주기를 스스로 관리합니다. 외부에서 필드를 직접 수정하지 못하고 반드시 메서드를 통해서만 상태가 바뀝니다.

```java
// 기존 — Service가 Token 대신 값을 세팅
token.setAccessToken("...");
token.setStatus("REVOKED");

// DDD — 객체 스스로 처리
auth.refresh(newCredential);  // 내부에서 credential 교체 + 이벤트 등록
auth.revoke();                // 내부에서 status = REVOKED + 이벤트 등록
auth.isExpired();             // 만료 여부를 스스로 판단
```

#### 2. Value Object — `AuthorizationId`, `TokenCredential`

의미 있는 값들을 묶어 불변 객체로 만들었습니다.

```java
// 기존 — 낱개 String으로 흩어져 있음
String mallId = "mymall";
String clientId = "abc123";
String accessToken = "token...";
String refreshToken = "refresh...";

// DDD — 의미 단위로 묶음
AuthorizationId id = new AuthorizationId("mymall", "abc123");
TokenCredential credential = new TokenCredential(accessToken, refreshToken, ...);

credential.isAccessTokenExpired(); // 만료 판단 로직도 같이 포함
credential.canBeRefreshed();
```

#### 3. 도메인 이벤트

상태가 바뀔 때 "이런 일이 일어났다"는 이벤트를 등록합니다.  
현재 `AuthorizationRevoked` 이벤트는 `WebhookEventService`가 구독해 인가 취소 처리에 사용합니다.

```
앱 삭제 Webhook 수신
    → WebhookController → AppUninstalledEvent 발행
    → WebhookEventService → AppAuthorizationService.revoke()
    → AppAuthorization.revoke() → AuthorizationRevoked 이벤트 등록
```

#### 4. Port & Adapter — `Cafe24OAuthPort` / `Cafe24OAuthGateway`

도메인이 HTTP 통신 방법을 몰라도 되도록 인터페이스(Port)와 구현체(Adapter)를 분리했습니다.

```
domain/service/Cafe24OAuthPort.java        ← 인터페이스 (도메인 레이어)
infrastructure/Cafe24OAuthGateway.java     ← 실제 HTTP 구현 (인프라 레이어)
```

`AppAuthorizationService`는 인터페이스만 바라보기 때문에, 테스트 시 가짜 구현체로 교체할 수 있고 Cafe24 API가 변경돼도 도메인 코드를 수정하지 않아도 됩니다.

---

## OAuth 흐름

```
1. GET /oauth/login
   → state를 세션에 저장
   → Cafe24 인가 서버로 리다이렉트

2. GET /oauth/callback?code=xxx&state=yyy
   → state 검증 (CSRF 방지)
   → Cafe24에서 토큰 발급
   → AppAuthorization 저장 (신규 생성 또는 기존 갱신)

3. POST /oauth/refresh
   → 저장된 리프레시 토큰으로 액세스 토큰 재발급
```

## Webhook 흐름

Cafe24는 이벤트마다 다른 Webhook URL을 등록할 수 있어, 기능별로 엔드포인트를 분리했다.
공통 검증(서명 확인, 필수값 확인)은 `AbstractCafe24WebhookController`가 담당한다.

```
POST /webhook/cafe24/app-uninstalled
   → x-api-key 헤더 검증
   → AppUninstalledEvent 발행 → AppAuthorization 상태를 즉시 REVOKED로 변경

POST /webhook/cafe24/products/created
POST /webhook/cafe24/products/updated
   → x-api-key 헤더 검증
   → ProductCreatedEvent / ProductUpdatedEvent 발행 → Cafe24 상품 상세 재조회 후 로컬 DB upsert

POST /webhook/cafe24/products/deleted
   → x-api-key 헤더 검증
   → ProductDeletedEvent 발행 → 로컬 DB에서만 삭제(Cafe24 재조회 없음)

POST /webhook/cafe24/orders/created
   → x-api-key 헤더 검증
   → OrderCreatedEvent 발행 → 원본 payload만 저장(이 요청 스레드에서는 Cafe24를 호출하지 않음)
   → OrderWebhookEventProcessor가 별도 주기로 Cafe24 재조회 후 반영(실패 시 백오프 재시도/DEAD 처리)

POST /webhook/cafe24/carriers/created
POST /webhook/cafe24/carriers/updated
POST /webhook/cafe24/carriers/deleted
   → x-api-key 헤더 검증
   → Carrier*Event 발행 → 중복 수신 이력만 기록(실제 반영은 CarrierSyncScheduler 주기 동기화가 전담)
```

각 흐름의 재시도 정책·실패 확인·수동 재처리 방법은 아래 "운영 가이드" 1~6번을 참고한다.

## 운영 가이드

운영 중 장애 대응이나 인수인계 시 참고할 내용을 정리했다. 아키텍처는 위 "DDD 전환 내용"을, 코드 스타일/테스트 규칙은 `CLAUDE.md`를 참고한다.

### 1. Webhook 수신 후 처리 흐름

모든 Webhook 컨트롤러는 `AbstractCafe24WebhookController.reject`로 공통 검증을 거친다.

1. `x-api-key` 헤더를 `Cafe24WebhookVerifier`로 검증한다. `CAFE24_WEBHOOK_API_KEY`가 비어 있으면 무조건 거부한다(fail-closed).
2. `event_no`와 리소스 식별자(productNo 등) 같은 필수값을 확인한다.
3. 검증 실패 시 401/400을, 통과 시 즉시 200을 반환하고 도메인 이벤트를 발행한다(Cafe24는 200을 못 받으면 재전송하므로 처리 성공 여부와 무관하게 검증을 통과하면 200을 줘야 한다).

이후 처리 방식은 이벤트 종류마다 다르다.

| Webhook | 요청 스레드 안에서 즉시 처리 | 비동기 처리 |
|---|---|---|
| `app-uninstalled` | 중복 체크 후 `AppAuthorization.revoke()` (인가 즉시 REVOKED) | 없음 |
| `products/created`, `products/updated` | 중복 체크 후 Cafe24 상품 상세 재조회 → 로컬 upsert | 없음 |
| `products/deleted` | 중복 체크 후 로컬 DB에서만 삭제(Cafe24 재조회 없음) | 없음 |
| `carriers/created`, `updated`, `deleted` | 이력 테이블(`cafe24_carrier_webhook_event`)에만 기록 | 실제 반영은 `CarrierSyncScheduler` 주기 동기화가 전담 |
| `orders/created` | 원본 payload만 `cafe24_order_webhook_event`에 `RECEIVED`로 저장 | `OrderWebhookEventProcessor`가 별도 주기로 Cafe24를 재조회해 order 테이블에 반영 |

주문 Webhook만 "수신"과 "반영"이 분리돼 있다 — 요청 스레드에서 Cafe24를 호출하지 않아 Webhook 응답 시간이 Cafe24/DB 상태에 영향받지 않는다. 나머지(앱 삭제·상품)는 요청 스레드 안에서 바로 반영한다.

중복 수신(Cafe24가 같은 이벤트를 여러 번 보내는 경우)은 `(event_no, mall_id, resource_id)` unique 제약으로 막는다. 동시에 같은 이벤트가 들어와 INSERT가 경합하면 `DataIntegrityViolationException`을 잡아 200으로 응답한다(재전송 방지).

### 2. 주문/상품/배송사 동기화 기준

Webhook은 트리거일 뿐 신뢰할 수 있는 유일한 경로가 아니므로(`CLAUDE.md` Webhook/Order Rules), 컨텍스트마다 주기 동기화가 별도로 돈다.

| 대상 | 스케줄러 | 주기 | 조회 기준 |
|---|---|---|---|
| 주문 | `OrderSyncScheduler` | `sync.order.fixed-rate-ms` | "지금 − `lookback-hours`" 이후 변경분만 — 커서를 저장하지 않고 매번 슬라이딩 윈도우로 조회 |
| 상품 | `ProductSyncScheduler` | `sync.product.cron` | 전체 페이지 재조회 + `reconcileMissingProducts`로 누락 상품 정리 |
| 배송사 | `CarrierSyncScheduler` | `sync.carrier.cron` | 전체 페이지 재조회(Cafe24 배송사 API에 변경시점 필터가 없음) |

공통 규칙:
- 모든 upsert는 멱등적이다(Order Rules) — 같은 데이터를 여러 번 반영해도 결과가 같다.
- Cafe24 API 호출(페이지 조회) 자체가 실패하면 그 실행은 즉시 중단하고 다음 스케줄에서 재시도한다. 예외를 스케줄러까지 전파시키지 않는다.
- 페이지 안의 개별 항목 upsert 실패는 해당 건만 건너뛰고 나머지는 계속 처리한다.
- 상품 동기화는 API 호출이 끝까지 성공했을 때만(`apiFailureCount == 0`) 누락 상품 정리를 수행한다 — 중간에 실패하면 정상 상품을 누락으로 오판할 수 있기 때문이다.
- 누락 상품 정리는 2단계다: 1차 누락 시 `STALE` 표시만 하고, STALE 상태에서 또 누락되면(2회 연속) 로컬에서 삭제한다. 다시 나타나면 upsert가 STALE 표시를 자동으로 해제한다.

### 3. 재시도 정책

스케줄 동기화(2번)는 별도의 "재시도" 개념이 없다 — 다음 스케줄 실행이 곧 재시도다.

주문 Webhook 이벤트(`OrderWebhookEvent`)만 별도의 재시도 상태 머신을 가진다.

```
RECEIVED ──(OrderWebhookEventProcessor)──▶ PROCESSING ──성공──▶ PROCESSED
                                              │
                                              ├─ 일시 오류 ──▶ FAILED ──nextRetryAt 이후 재시도──┐
                                              │                  │                              │
                                              │       재시도 횟수 > max-retry-count            (PROCESSING으로)
                                              │                  ▼
                                              │                DEAD
                                              │
                                              └─ 영구 오류(HTTP 400) ──▶ DEAD (즉시, 남은 재시도 예산 무시)
```

- 백오프 간격: 1분 → 5분 → 15분 → 1시간, 이후 1시간 유지(`OrderWebhookEvent.RETRY_BACKOFF_SCHEDULE` 코드 상수 — 설정 불가)
- 최대 재시도 횟수: `worker.order-webhook.max-retry-count` 초과 시 `DEAD`로 전환
- "영구 오류"는 Cafe24 응답이 `400 Bad Request`인 경우만 해당한다. 401/403/429/5xx는 토큰 재인증 문제나 Cafe24 측 일시 장애일 수 있어 일시 오류로 보고 backoff 재시도한다.
- `PROCESSING` 상태로 5분(`OrderWebhookEvent.PROCESSING_STALE_TIMEOUT`) 이상 머물면 앱 크래시로 간주해 재시도 대상에 다시 포함된다(Cafe24 호출 타임아웃 합계 8초보다 충분히 길게 잡은 값).

### 4. Backfill 실행 방법

인증 없는 관리용 동기 API다(9번 참고). 호출 즉시 동기화를 실행하고 끝날 때까지 응답을 기다린다.

```
POST /admin/backfill/orders?startDate=2026-06-01&endDate=2026-06-25
POST /admin/backfill/products
POST /admin/backfill/carriers
```

- `orders`만 기간(`startDate`~`endDate`, ISO 날짜)을 받는다. 상품/배송사는 항상 전체 재조회라 기간 파라미터가 없다.
- 모두 평소 스케줄 동기화와 같은 코드 경로(`syncFromCafe24`/`backfillFromCafe24`)를 타므로 멱등적이고, 실행 결과는 `sync_run_status` 테이블에도 동일하게 기록된다.
- 동기 호출이므로 대량 기간을 백필할 때는 응답이 늦게 올 수 있다(호출하는 쪽 타임아웃을 충분히 잡아야 한다).

### 5. 실패 이벤트 확인 방법

**운영 지표 API**

```
GET /admin/metrics
```

- `webhook.unprocessedCount`: 아직 반영되지 않고 재시도 대상으로 남은 주문 Webhook 이벤트 수(RECEIVED+PROCESSING+FAILED)
- `webhook.failedCount` / `webhook.deadCount`: 그중 FAILED / DEAD 건수
- `webhook.totalRetryCount`: 전체 이벤트의 재시도 누적 합계
- `syncStatuses[]`: ORDER/PRODUCT/CARRIER별 `lastRunAt`(마지막 실행 시각), `lastSuccessAt`(Cafe24 API 호출이 끝까지 성공한 마지막 시각), `lastProcessedCount`/`lastFailedCount`(개별 항목 단위), `lastApiFailureCount`(0이면 정상, 1이면 그 실행에서 Cafe24 API 호출 자체가 실패), `lastErrorMessage`

`lastRunAt`과 `lastSuccessAt`이 계속 벌어지면 Cafe24 API 호출이 반복적으로 실패하고 있다는 뜻이다.

**로그**

- `Order/Product/Carrier sync Cafe24 API 호출 실패, 이번 실행 중단` — 페이지 조회 자체 실패
- `... sync item failed, continuing` / `... 1건 실패, 다음 건 계속 진행` — 개별 항목 실패
- `Order webhook event 처리 실패` — 주문 Webhook 재처리 실패(다음 줄 스택트레이스에서 원인 확인)

**DB 직접 조회**

```sql
SELECT * FROM cafe24_order_webhook_event WHERE status IN ('FAILED','DEAD') ORDER BY received_at;
SELECT * FROM sync_run_status;
```

### 6. 수동 재처리 방법

개별 이벤트(특정 주문 1건, 특정 상품 1건)를 다시 처리시키는 전용 API는 없다. 실제로는 다음 방법뿐이다.

1. **주문**: Source of Truth가 Cafe24 Orders API이므로(Order Rules), 특정 주문이 반영되지 않았다면 해당 날짜를 포함한 기간으로 `POST /admin/backfill/orders`를 호출하는 것이 가장 안전하다. upsert가 멱등적이라 다른 주문에 영향을 주지 않는다.
2. **DEAD로 멈춘 주문 Webhook 이벤트를 다시 시도시키고 싶다면**: 먼저 `error_message` 컬럼으로 원인을 확인하고, 원인(예: 인가 만료)이 해소된 뒤에만 DB에서 직접 상태를 되돌린다.
   ```sql
   UPDATE cafe24_order_webhook_event
   SET status = 'RECEIVED', retry_count = 0, next_retry_at = NULL
   WHERE id = ?;
   ```
   다음 `OrderWebhookEventProcessor` 주기(`worker.order-webhook.fixed-delay-ms`)에 자동으로 다시 시도된다. 원인이 해소되지 않은 채로 되돌리면 같은 이유로 다시 실패해 재시도 횟수만 소모한다.
3. **상품/배송사**: 개별 `product_no`/`shipping_carrier_code` 재처리 API가 없다. `POST /admin/backfill/products` 또는 `/carriers`로 전체를 다시 동기화한다(매일 도는 스케줄과 동일한 동작이라 안전하다).

### 7. 운영 설정값 목록

**환경변수**

| 변수명 | 설명 |
|---|---|
| `CAFE24_CLIENT_ID` | Cafe24 앱 클라이언트 ID |
| `CAFE24_CLIENT_SECRET` | Cafe24 앱 클라이언트 Secret |
| `CAFE24_MALL_ID` | 연동할 쇼핑몰 ID |
| `CAFE24_REDIRECT_URI` | OAuth 콜백 URI |
| `CAFE24_API_VERSION` | Cafe24 API 호출 시 `X-Cafe24-Api-Version` 헤더에 그대로 들어가는 값(예: `2025-09-01`). 기본값이 없어 누락 시 모든 Cafe24 API 호출(주문/상품/배송사 동기화 포함)이 실패한다 |
| `CAFE24_WEBHOOK_API_KEY` | Webhook 서명 검증 키. 비어 있으면 모든 Webhook 요청이 거부됨(fail-closed) |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | MySQL 접속 정보 |
| `SPRING_PROFILES_ACTIVE` | 미지정 시 `local` 적용. 운영 배포 시 반드시 `prod` 지정 |

**`application.yml` 설정값**(운영 중 배포 없이 조정하려면 환경변수로 덮어쓸 수 있다 — 정확한 현재 값은 배포된 `application.yml`을 확인할 것)

| 키 | 의미 |
|---|---|
| `worker.order-webhook.fixed-delay-ms` | 주문 Webhook 재처리 워커 실행 간격 |
| `worker.order-webhook.batch-size` | 한 번에 조회하는 재시도 대상 이벤트 수 |
| `worker.order-webhook.max-retry-count` | 초과 시 DEAD 전환 |
| `sync.order.fixed-rate-ms` | 주문 동기화 스케줄러 실행 간격 |
| `sync.order.lookback-hours` | 주문 동기화가 조회하는 변경 시점 하한 |
| `sync.product.cron` | 상품 전체 동기화 cron |
| `sync.carrier.cron` | 배송사 전체 동기화 cron |

**코드 상수**(설정으로 바꿀 수 없고 변경하려면 코드 수정이 필요하다)

| 위치 | 값 | 의미 |
|---|---|---|
| `OrderWebhookEvent.RETRY_BACKOFF_SCHEDULE` | 1분→5분→15분→1시간(이후 유지) | 주문 Webhook 재시도 backoff |
| `OrderWebhookEvent.PROCESSING_STALE_TIMEOUT` | 5분 | PROCESSING 멈춤(크래시 추정) 감지 기준 |
| `OrderService`/`ProductService`/`CarrierService`의 `SYNC_PAGE_SIZE` | 100 | Cafe24 페이지 조회 단위 |
| `AppConfig.restTemplate` | connect 3초 / read 5초 | 모든 Cafe24 API 호출 공통 타임아웃 |

### 8. 인가(OAuth) 토큰 갱신/만료 대응

모든 Cafe24 API 호출(동기화, Webhook 재조회, Backfill)은 `AppAuthorizationService.getValidCredential(mallId)`를 거친다.

- 액세스 토큰이 만료됐지만 리프레시 토큰이 살아있으면 자동으로 갱신 후 진행된다(별도 운영 조치 불필요).
- 해당 `mallId`로 인가 자체가 없으면(`/oauth/login`을 한 번도 안 거쳤거나, 앱 삭제 Webhook으로 REVOKED된 뒤 재인가하지 않은 경우) `IllegalStateException`이 발생해 그 실행의 동기화/Webhook 처리가 전부 실패한다 — `/oauth/login`부터 다시 진행해야 한다.
- 리프레시 토큰까지 만료/무효화되면 Cafe24가 401을 반환한다. 주문 Webhook 재처리는 이를 영구 오류(400)로 분류하지 않아 backoff 재시도를 반복하다 `max-retry-count` 소진 후 DEAD가 되고, 스케줄 동기화는 다음 주기에도 같은 이유로 계속 실패한다. **재인가(`/oauth/login`) 전까지는 재시도해도 해결되지 않는다.**

### 9. 운영 엔드포인트 보안 주의사항

`/admin/backfill/*`, `/admin/metrics`는 애플리케이션 레벨 인증 없이 호출 가능하다(`AdminBackfillController`/`AdminMetricsController` 설계). 운영 환경에 배포할 때는 반드시 네트워크 레벨(방화벽, 리버스 프록시 IP 화이트리스트, VPN 등)로 접근을 제한해야 한다.

### 10. 참고: DB 테이블 목록

| 테이블 | 용도 |
|---|---|
| `cafe24_token` | 인가(`AppAuthorization`) — 토큰 쌍 + 상태 |
| `cafe24_order` | 동기화된 주문 |
| `cafe24_product` | 동기화된 상품 |
| `cafe24_carrier` | 동기화된 배송사 |
| `cafe24_order_webhook_event` | 주문 Webhook 원본 + 재시도 상태(RECEIVED/PROCESSING/PROCESSED/FAILED/DEAD) |
| `cafe24_product_webhook_event` | 앱 삭제·상품 Webhook 중복 수신 체크용 이력(테이블명은 상품 기준이지만 앱 삭제 이벤트도 함께 저장됨) |
| `cafe24_carrier_webhook_event` | 배송사 Webhook 중복 수신 체크용 이력(반영은 안 하고 기록만 함) |
| `sync_run_status` | mallId+대상별 마지막 동기화 실행 결과 1건(이력 누적 아님) |

---

## 개인 메모

```
도메인 = 이 소프트웨어가 풀려는 현실 세계의 문제 (HTTP도 JPA도 전혀 없음 순수한 비즈니스 규칙만 존재)
이 프로젝트의 도메인은 "Cafe24 쇼핑몰에 앱을 설치하고 인가를 관리하는 것"

요청 들어옴
    ↓
presentation   "POST /oauth/callback 왔다, code랑 state 꺼내서 넘길게"
    ↓
application    "1. 토큰 발급해, 2. 인가 만들어, 3. 저장해"
    ↓
domain         "잠깐, REVOKED면 갱신 안 돼" (규칙 검사)
    ↓
infrastructure "실제로 HTTP 쏘고, DB에 저장할게"


┌─────────────────────────────────────┐
│  presentation  (HTTP 요청/응답)      │  ← 컨트롤러
├─────────────────────────────────────┤
│  application   (순서 조율)           │  ← 서비스
├─────────────────────────────────────┤
│  domain        (핵심 비즈니스 로직)  │  ← 엔티티, 이벤트, 인터페이스
├─────────────────────────────────────┤
│  infrastructure (기술적인 구현)      │  ← DB, HTTP 통신
└─────────────────────────────────────┘


기존 Service 안에 있던 것들
        ↓ 분리
┌──────────────────────────────────────────────────────┐
│ presentation  Controller가 하던 것 (HTTP 처리)         │
├──────────────────────────────────────────────────────┤
│ application   Service가 하던 것 (순서 조율)             │
├──────────────────────────────────────────────────────┤
│ domain        Entity가 해야 했던 것 (비즈니스 규칙)      │
├──────────────────────────────────────────────────────┤
│ infrastructure Repository + 외부 API 호출             │
└──────────────────────────────────────────────────────┘


기존                         새로운 DDD 구조
─────────────────────────    ──────────────────────────────────
OAuthController.java     →   presentation/OAuthController.java
WebhookController.java   →   presentation/WebhookController.java

TokenService.java        →   application/AppAuthorizationService.java
OAuthService.java        →   infrastructure/Cafe24OAuthGateway.java
                             (HTTP 통신 부분만 따로 뺀 것)

TokenRepository.java     →   domain/AppAuthorizationRepository.java (인터페이스)
                             infrastructure/AppAuthorizationRepositoryAdapter.java (구현)

Token.java (빈 껍데기)   →   domain/AppAuthorization.java (행위 포함)
                             domain/AuthorizationId.java (mallId+clientId 묶음)
                             domain/TokenCredential.java (토큰 쌍 묶음)

TokenResponse.java (DTO) →   infrastructure/Cafe24TokenResponse.java (외부 API용)
```