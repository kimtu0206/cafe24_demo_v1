# Cafe24 Demo v1

Cafe24 OAuth 인가 흐름을 구현한 Spring Boot 프로젝트입니다.  
기존 Layered Architecture(Controller-Service-Repository)에서 **DDD(Domain-Driven Design) 기반 구조**로 전환했습니다.

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
| `CAFE24_WEBHOOK_API_KEY` | Webhook 서명 검증 키 (로컬 개발 시 생략 가능) |
| `DB_URL` | MySQL 접속 URL |
| `DB_USERNAME` | DB 사용자명 |
| `DB_PASSWORD` | DB 비밀번호 |

---

## API 엔드포인트

| 메서드 | URL | 설명 |
|---|---|---|
| `GET` | `/oauth/login` | OAuth 로그인 시작 (Cafe24 인가 서버로 리다이렉트) |
| `GET` | `/oauth/callback` | OAuth 콜백 수신 및 토큰 저장 |
| `POST` | `/oauth/refresh` | 액세스 토큰 수동 갱신 |
| `POST` | `/webhook/cafe24/app-uninstalled` | Cafe24 앱 삭제 Webhook 수신 |
| `POST` | `/webhook/cafe24/products/created` | Cafe24 상품 생성 Webhook 수신 |
| `POST` | `/webhook/cafe24/products/updated` | Cafe24 상품 수정 Webhook 수신 |
| `POST` | `/webhook/cafe24/products/deleted` | Cafe24 상품 삭제 Webhook 수신 |

---

## 패키지 구조

```
src/main/java/org/example/cafe24_demo_v1/
│
├── authorization/                          # OAuth 인가 컨텍스트
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
│   │       └── AppAuthorizationService.java
│   │
│   ├── infrastructure/                     # 기술적 구현 (DB, HTTP)
│   │   ├── external/
│   │   │   ├── Cafe24OAuthGateway.java     # Cafe24 OAuth API 어댑터
│   │   │   └── Cafe24TokenResponse.java    # 외부 API 응답 DTO
│   │   └── persistence/
│   │       ├── AppAuthorizationEntity.java          # JPA 엔티티
│   │       ├── AppAuthorizationJpaRepository.java   # Spring Data JPA 인터페이스
│   │       ├── AppAuthorizationMapper.java           # 도메인 ↔ JPA 변환
│   │       └── AppAuthorizationRepositoryAdapter.java # 저장소 구현체
│   │
│   └── presentation/                       # HTTP 입출력
│       └── OAuthController.java
│
├── webhook/                                # Webhook 컨텍스트
│   ├── domain/event/
│   │   └── AppUninstalledEvent.java        # 앱 삭제 이벤트
│   ├── application/
│   │   └── WebhookEventService.java        # 이벤트 구독 및 처리
│   ├── infrastructure/
│   │   └── Cafe24WebhookVerifier.java      # 서명 검증
│   └── presentation/
│       └── WebhookController.java
│
└── shared/                                 # 공용 설정
    └── config/
        └── Cafe24Properties.java
```

---

## DDD 전환 내용

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
   → AppUninstalledEvent 발행 → AppAuthorization 상태를 REVOKED로 변경

POST /webhook/cafe24/products/created
POST /webhook/cafe24/products/updated
POST /webhook/cafe24/products/deleted
   → x-api-key 헤더 검증
   → ProductCreatedEvent / ProductUpdatedEvent / ProductDeletedEvent 발행 → 로컬 DB 반영
```

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