# Cafe24 연동 프로젝트 학습 및 디버깅 가이드

이 문서는 Java / Spring Boot / MyBatis 경험자가 이 프로젝트의 Cafe24 연동, JPA, DDD 기반 구조를 디버깅하면서 이해하기 위한 흐름 정리 문서입니다.

## 1. 전체 시스템 흐름

프로젝트의 큰 구조는 다음과 같습니다.

```text
presentation  ->  application  ->  domain  ->  infrastructure
Controller        Service          Model/Port  JPA/HTTP/External API
```

- `presentation`: HTTP 요청을 받는 Controller
- `application`: 유스케이스 흐름을 조율하는 Service
- `domain`: 비즈니스 상태와 규칙을 가진 모델, 외부 의존성 인터페이스
- `infrastructure`: JPA, DB, Cafe24 API 호출 같은 기술 구현체

주요 bounded context는 다음과 같습니다.

- `authorization`: Cafe24 OAuth 토큰 발급, 갱신, 폐기
- `product`: 상품 등록, 수정, 삭제, 동기화
- `order`: 주문 동기화, 주문 웹훅 비동기 처리
- `carrier`: 배송사 등록, 동기화
- `webhook`: Cafe24 웹훅 수신, 검증, 이벤트 발행
- `monitoring`: 동기화 실행 결과 기록
- `admin`: 백필, 메트릭 조회

## 2. OAuth 인증 흐름

디버깅 시작점:

- `org.example.cafe24_demo_v1.authorization.presentation.OAuthController`

호출 순서:

```text
OAuthController.login()
  -> Cafe24 인증 URL로 redirect

OAuthController.callback()
  -> AppAuthorizationService.grant()
  -> Cafe24OAuthGateway.issueToken()
  -> AppAuthorization.grant()
  -> AppAuthorizationRepositoryAdapter.save()
  -> AppAuthorizationJpaRepository.save()
```

중요 클래스:

- `OAuthController`: `/oauth/login`, `/oauth/callback`, `/oauth/refresh` 진입점
- `AppAuthorizationService`: 인증 유스케이스 조율
- `AppAuthorization`: 인증 상태를 가진 Aggregate Root
- `TokenCredential`: access token, refresh token, 만료시간을 가진 값 객체
- `Cafe24OAuthGateway`: Cafe24 OAuth API 실제 호출
- `AppAuthorizationRepositoryAdapter`: 도메인 Repository를 JPA로 구현

디버깅 포인트:

- `OAuthController.callback()`
- `AppAuthorizationService.grant()`
- `Cafe24OAuthGateway.issueToken()`
- `AppAuthorization.grant()`
- `AppAuthorizationRepositoryAdapter.save()`

## 3. 상품 REST API 흐름

디버깅 시작점:

- `org.example.cafe24_demo_v1.product.presentation.ProductController`

상품 등록 호출 순서:

```text
POST /products
  -> ProductController.register()
  -> ProductService.register()
  -> AppAuthorizationService.getValidCredential()
  -> Cafe24ProductClient.createProduct()
  -> ProductRepositoryAdapter.save()
  -> ProductJpaRepository.save()
```

상품 수정 호출 순서:

```text
PUT /products/{productNo}
  -> ProductController.update()
  -> ProductService.update()
  -> AppAuthorizationService.getValidCredential()
  -> Cafe24ProductClient.updateProduct()
  -> ProductService.upsert()
  -> ProductRepositoryAdapter.findByMallIdAndProductNo()
  -> ProductRepositoryAdapter.save()
```

상품 삭제 호출 순서:

```text
DELETE /products/{productNo}
  -> ProductController.delete()
  -> ProductService.delete()
  -> AppAuthorizationService.getValidCredential()
  -> Cafe24ProductClient.deleteProduct()
  -> ProductRepositoryAdapter.deleteByMallIdAndProductNo()
```

중요 클래스:

- `ProductController`: `/products` REST API 진입점
- `ProductService`: 상품 유스케이스 조율
- `Product`: 상품 도메인 모델
- `ProductRegistration`: Cafe24 상품 등록/수정 입력 값
- `Cafe24ProductPort`: 상품 Cafe24 API 포트
- `Cafe24ProductClient`: Cafe24 상품 API 실제 호출
- `ProductRepository`: 도메인 저장소 인터페이스
- `ProductRepositoryAdapter`: JPA 저장소 구현체
- `ProductEntity`: JPA Entity
- `ProductMapper`: Domain <-> Entity 변환

디버깅 포인트:

- `ProductController.register()`
- `ProductService.register()`
- `AppAuthorizationService.getValidCredential()`
- `Cafe24ProductClient.exchange()`
- `ProductRepositoryAdapter.save()`
- `ProductService.upsert()`

## 4. 상품 스케줄러 흐름

디버깅 시작점:

- `org.example.cafe24_demo_v1.product.infrastructure.scheduler.ProductSyncScheduler`

설정:

```yaml
sync:
  product:
    cron: "0 0 10 * * *"
```

호출 순서:

```text
ProductSyncScheduler.syncProducts()
  -> ProductService.syncFromCafe24()
  -> AppAuthorizationService.getValidCredential()
  -> ProductService.syncPages()
  -> Cafe24ProductClient.getProducts()
  -> ProductService.upsert()
  -> ProductRepositoryAdapter.save()
  -> SyncMetricsService.recordRun()
```

특징:

- Cafe24 상품 목록을 페이지 단위로 조회합니다.
- 조회된 상품은 로컬 DB에 upsert합니다.
- Cafe24 응답에 없는 로컬 상품은 `missingSince`로 1차 누락 표시 후, 다음 동기화에서도 없으면 삭제합니다.

## 5. 웹훅 공통 흐름

공통 검증 시작점:

- `org.example.cafe24_demo_v1.webhook.presentation.AbstractCafe24WebhookController`

공통 처리:

```text
WebhookController
  -> AbstractCafe24WebhookController.reject()
  -> Cafe24WebhookVerifier.verify()
  -> ApplicationEventPublisher.publishEvent()
  -> WebhookEventService.@EventListener method
```

중요 클래스:

- `AbstractCafe24WebhookController`: API key 검증, 필수 값 검증
- `Cafe24WebhookVerifier`: Cafe24 웹훅 API key 검증
- `ProductWebhookController`: 상품 웹훅 수신
- `OrderWebhookController`: 주문 웹훅 수신
- `CarrierWebhookController`: 배송사 웹훅 수신
- `AppUninstallWebhookController`: 앱 삭제 웹훅 수신
- `WebhookEventService`: 웹훅 이벤트 구독 및 처리

주의:

- Spring `@EventListener`는 기본적으로 동기 실행입니다.
- 즉 Controller에서 `publishEvent()`를 호출하면 리스너 처리가 같은 흐름에서 실행됩니다.

## 6. 상품 웹훅 흐름

디버깅 시작점:

- `org.example.cafe24_demo_v1.webhook.presentation.ProductWebhookController`

상품 생성/수정 웹훅 호출 순서:

```text
POST /webhook/cafe24/products/created
POST /webhook/cafe24/products/updated
  -> ProductWebhookController.created()/updated()
  -> AbstractCafe24WebhookController.reject()
  -> ApplicationEventPublisher.publishEvent()
  -> WebhookEventService.onProductCreated()/onProductUpdated()
  -> WebhookEventRepository.exists()
  -> WebhookEventRepository.save()
  -> ProductService.upsertFromWebhook()
  -> AppAuthorizationService.getValidCredential()
  -> Cafe24ProductClient.getProduct()
  -> ProductService.upsert()
  -> ProductRepositoryAdapter.save()
```

상품 삭제 웹훅 호출 순서:

```text
POST /webhook/cafe24/products/deleted
  -> ProductWebhookController.deleted()
  -> WebhookEventService.onProductDeleted()
  -> WebhookEventRepository.save()
  -> ProductService.deleteFromWebhook()
  -> ProductRepositoryAdapter.deleteByMallIdAndProductNo()
```

주의:

- 상품 생성/수정 웹훅은 Cafe24에서 상세 상품을 다시 조회합니다.
- 상품 삭제 웹훅은 Cafe24 API를 다시 호출하지 않고 로컬 DB에서만 삭제합니다.
- `WebhookEventService.onProductCreated()`와 `onProductUpdated()`에 `@Transactional`이 있으므로, 그 안에서 호출되는 Cafe24 외부 API 호출도 같은 트랜잭션 흐름 안에서 실행될 수 있습니다.

## 7. 주문 웹훅 비동기 처리 흐름

주문 웹훅은 상품 웹훅과 다르게 즉시 주문 테이블에 반영하지 않습니다.

수신 흐름:

```text
POST /webhook/cafe24/orders/created
  -> OrderWebhookController.created()
  -> AbstractCafe24WebhookController.reject()
  -> ApplicationEventPublisher.publishEvent()
  -> WebhookEventService.onOrderCreated()
  -> OrderWebhookEventService.saveRaw()
  -> OrderWebhookEvent.receive()
  -> OrderWebhookEventRepositoryAdapter.save()
```

비동기 처리 흐름:

```text
OrderWebhookEventProcessor.process()
  -> OrderWebhookEventService.processUnprocessed()
  -> OrderWebhookEventRepository.findRetryableEvents()
  -> OrderWebhookEvent.markProcessing()
  -> OrderWebhookEventRepository.save()
  -> OrderService.upsertFromWebhook()
  -> AppAuthorizationService.getValidCredential()
  -> Cafe24OrderClient.getOrder()
  -> OrderService.upsert()
  -> OrderRepositoryAdapter.save()
  -> OrderWebhookEvent.markProcessed()
  -> OrderWebhookEventRepository.save()
```

실패 시:

```text
Cafe24 API 일시 오류
  -> OrderWebhookEvent.markFailed()
  -> retryCount 증가
  -> nextRetryAt 설정

Cafe24 API 400 오류
  -> OrderWebhookEvent.markDead()
  -> 재시도 중단
```

중요 클래스:

- `OrderWebhookController`: 주문 웹훅 수신
- `OrderWebhookEventService`: 원본 저장, 재시도 처리
- `OrderWebhookEvent`: 웹훅 처리 상태 머신
- `OrderWebhookEventProcessor`: 주기적 비동기 처리 worker
- `OrderService`: 주문 Cafe24 조회 및 upsert
- `Cafe24OrderClient`: Cafe24 주문 API 호출

디버깅 포인트:

- `OrderWebhookController.created()`
- `WebhookEventService.onOrderCreated()`
- `OrderWebhookEventService.saveRaw()`
- `OrderWebhookEventProcessor.process()`
- `OrderWebhookEventService.processUnprocessed()`
- `OrderWebhookEvent.markProcessing()`
- `OrderService.upsertFromWebhook()`
- `Cafe24OrderClient.exchange()`
- `OrderWebhookEvent.markProcessed()`
- `OrderWebhookEvent.markFailed()`
- `OrderWebhookEvent.markDead()`

## 8. 주문 정기 동기화 흐름

디버깅 시작점:

- `org.example.cafe24_demo_v1.order.infrastructure.scheduler.OrderSyncScheduler`

설정:

```yaml
sync:
  order:
    fixed-rate-ms: 86400000
    lookback-hours: 48
```

호출 순서:

```text
OrderSyncScheduler.syncOrders()
  -> OrderService.syncFromCafe24()
  -> AppAuthorizationService.getValidCredential()
  -> Cafe24OrderClient.getOrders()
  -> OrderService.upsert()
  -> OrderRepositoryAdapter.save()
  -> SyncMetricsService.recordRun()
```

특징:

- 마지막 커서를 저장하지 않고 `현재 시각 - lookback-hours` 기준으로 반복 조회합니다.
- 주문 upsert가 멱등적이므로 중복 조회를 허용하는 방식입니다.
- 웹훅 누락을 보완하는 안전망 역할입니다.

## 9. 배송사 흐름

배송사 등록 REST 흐름:

```text
POST /carriers
  -> CarrierController.register()
  -> CarrierService.registerCarrier()
  -> AppAuthorizationService.getValidCredential()
  -> Cafe24CarrierClient.createCarrier()
  -> CarrierRepositoryAdapter.save()
```

배송사 정기 동기화 흐름:

```text
CarrierSyncScheduler.syncCarriers()
  -> CarrierService.syncFromCafe24()
  -> AppAuthorizationService.getValidCredential()
  -> Cafe24CarrierClient.getCarriers()
  -> CarrierService.upsert()
  -> CarrierRepositoryAdapter.save()
  -> SyncMetricsService.recordRun()
```

배송사 웹훅 흐름:

```text
CarrierWebhookController.created()/updated()/deleted()
  -> ApplicationEventPublisher.publishEvent()
  -> WebhookEventService.onCarrierCreated()/onCarrierUpdated()/onCarrierDeleted()
  -> CarrierWebhookEventRepository.save()
```

주의:

- 배송사 웹훅은 실제 `cafe24_carrier` 테이블을 즉시 변경하지 않습니다.
- 웹훅 이력만 저장하고, 실제 배송사 데이터 반영은 `CarrierSyncScheduler`가 담당합니다.

## 10. DDD 관점 정리

### Aggregate Root

- `AppAuthorization`
  - 인증 생명주기 관리
  - `grant()`, `refresh()`, `revoke()`, `getValidCredential()`
- `Product`
  - 상품 snapshot 관리
  - `register()`, `applySnapshot()`, `markMissing()`
- `Order`
  - Cafe24 주문 snapshot 관리
  - `register()`, `applySnapshot()`
- `OrderWebhookEvent`
  - 주문 웹훅 처리 상태 머신
  - `receive()`, `markProcessing()`, `markProcessed()`, `markFailed()`, `markDead()`
- `Carrier`
  - 배송사 snapshot 관리
  - `register()`, `applySnapshot()`

### Application Service

- `AppAuthorizationService`
- `ProductService`
- `OrderService`
- `OrderWebhookEventService`
- `CarrierService`
- `WebhookEventService`

Application Service는 비즈니스 규칙 자체보다는 유스케이스의 순서를 조율합니다.

예:

```text
토큰 조회
  -> Cafe24 API 호출
  -> 도메인 객체 생성/변경
  -> Repository 저장
```

### Domain Port

- `Cafe24OAuthPort`
- `Cafe24ProductPort`
- `Cafe24OrderPort`
- `Cafe24CarrierPort`

Application/Domain 입장에서는 Cafe24 API 호출이 필요하다는 사실만 알고, 실제 HTTP 구현은 모릅니다.

### Infrastructure Adapter

- `Cafe24OAuthGateway`
- `Cafe24ProductClient`
- `Cafe24OrderClient`
- `Cafe24CarrierClient`
- `*RepositoryAdapter`
- `*JpaRepository`
- `*Entity`
- `*Mapper`

외부 API, JPA, DB 같은 기술 세부 구현이 여기에 있습니다.

## 11. 기존 Layered Architecture와 차이

기존 Layered Architecture는 보통 다음 형태입니다.

```text
Controller -> Service -> Repository -> Entity
```

이 프로젝트는 다음 형태입니다.

```text
Controller
  -> Application Service
  -> Domain Model / Domain Repository Interface / Domain Port
  -> Infrastructure Adapter
  -> JPA Entity / Cafe24 HTTP Client
```

핵심 차이:

- Service가 JPA Entity를 직접 다루지 않습니다.
- Domain은 JPA, RestTemplate, DB 테이블을 모릅니다.
- Repository는 domain 패키지에 인터페이스로 있고, 실제 구현은 infrastructure에 있습니다.
- Cafe24 API 호출도 domain port 인터페이스 뒤에 숨깁니다.

## 12. JPA 관점에서 주의할 점

### 12.1 도메인 모델과 JPA Entity가 분리되어 있음

예:

```text
Product       -> 순수 도메인 모델
ProductEntity -> JPA Entity
ProductMapper -> 둘 사이 변환
```

MyBatis에서는 보통 SQL 결과를 DTO나 Entity에 직접 매핑합니다.

이 프로젝트에서는 다음 흐름을 거칩니다.

```text
DB row
  -> ProductEntity
  -> ProductMapper.toDomain()
  -> Product
```

저장은 반대입니다.

```text
Product
  -> ProductMapper.toEntity()
  -> ProductEntity
  -> ProductJpaRepository.save()
```

### 12.2 Dirty Checking이 제한적으로만 체감됨

JPA의 Dirty Checking은 영속성 컨텍스트가 관리하는 Entity가 변경될 때 동작합니다.

하지만 이 프로젝트의 Application Service가 주로 변경하는 객체는 JPA Entity가 아니라 순수 도메인 객체입니다.

예:

```java
existing.applySnapshot(...);
repository.save(existing);
```

즉 `existing`은 `ProductEntity`가 아니라 `Product`입니다.

그래서 변경 후 `repository.save(existing)`를 명시적으로 호출합니다.

### 12.3 Transaction 범위

트랜잭션이 명확히 있는 곳:

- `AppAuthorizationService.grant()`
- `AppAuthorizationService.refresh()`
- `AppAuthorizationService.revoke()`
- `AppAuthorizationService.getValidCredential()`
- `WebhookEventService.onAppUninstalled()`
- `WebhookEventService.onProductCreated()`
- `WebhookEventService.onProductUpdated()`
- `WebhookEventService.onProductDeleted()`
- `WebhookEventService.onCarrierCreated()`
- `WebhookEventService.onCarrierUpdated()`
- `WebhookEventService.onCarrierDeleted()`
- `OrderWebhookEventService.saveRaw()`
- `ProductService.deleteFromWebhook()`

트랜잭션이 일부러 없는 주요 흐름:

- `ProductService.register()`
- `ProductService.update()`
- `ProductService.delete()`
- `ProductService.syncFromCafe24()`
- `OrderService.syncFromCafe24()`
- `OrderService.backfillFromCafe24()`
- `CarrierService.registerCarrier()`
- `CarrierService.syncFromCafe24()`

이유:

- Cafe24 API 호출 같은 외부 I/O 동안 DB 커넥션을 오래 점유하지 않기 위해서입니다.
- 각 `repository.save()`는 Spring Data JPA Repository 내부 트랜잭션으로 독립 실행됩니다.

주의할 예외:

- 상품 웹훅은 `WebhookEventService.onProductCreated()`에 `@Transactional`이 있고 그 안에서 `ProductService.upsertFromWebhook()`을 호출합니다.
- 이 경우 Cafe24 외부 API 호출이 웹훅 이벤트 저장 트랜잭션 안에서 실행될 수 있습니다.
- 디버깅할 때 트랜잭션 경계를 반드시 확인해야 합니다.

## 13. 실제 디버깅 체크리스트

아래 순서대로 보면 가장 빠르게 전체 구조를 이해할 수 있습니다.

### 1단계: 애플리케이션 시작과 Scheduler 활성화 확인

- `Cafe24DemoV1Application`
- `@EnableScheduling`
- `application.yml`
- `sync.product.cron`
- `sync.carrier.cron`
- `sync.order.fixed-rate-ms`
- `worker.order-webhook.fixed-delay-ms`

### 2단계: OAuth 인증 흐름

- `OAuthController.login()`
- `OAuthController.callback()`
- `AppAuthorizationService.grant()`
- `Cafe24OAuthGateway.issueToken()`
- `AppAuthorization.grant()`
- `AppAuthorizationRepositoryAdapter.save()`

### 3단계: 상품 등록 REST 흐름

- `ProductController.register()`
- `ProductService.register()`
- `AppAuthorizationService.getValidCredential()`
- `Cafe24ProductClient.createProduct()`
- `Cafe24ProductClient.exchange()`
- `ProductRepositoryAdapter.save()`
- `ProductMapper.toEntity()`
- `ProductJpaRepository.save()`

### 4단계: 상품 수정/삭제 REST 흐름

- `ProductController.update()`
- `ProductService.update()`
- `ProductService.upsert()`
- `Product.applySnapshot()`
- `ProductRepositoryAdapter.save()`
- `ProductController.delete()`
- `ProductService.delete()`
- `Cafe24ProductClient.deleteProduct()`
- `ProductRepositoryAdapter.deleteByMallIdAndProductNo()`

### 5단계: 상품 웹훅 흐름

- `ProductWebhookController.created()`
- `ProductWebhookController.updated()`
- `ProductWebhookController.deleted()`
- `AbstractCafe24WebhookController.reject()`
- `ApplicationEventPublisher.publishEvent()`
- `WebhookEventService.onProductCreated()`
- `WebhookEventService.onProductUpdated()`
- `WebhookEventService.onProductDeleted()`
- `ProductService.upsertFromWebhook()`
- `ProductService.deleteFromWebhook()`

### 6단계: 주문 웹훅 수신 흐름

- `OrderWebhookController.created()`
- `WebhookEventService.onOrderCreated()`
- `OrderWebhookEventService.saveRaw()`
- `OrderWebhookEvent.receive()`
- `OrderWebhookEventRepositoryAdapter.save()`

### 7단계: 주문 웹훅 비동기 처리 흐름

- `OrderWebhookEventProcessor.process()`
- `OrderWebhookEventService.processUnprocessed()`
- `OrderWebhookEventRepository.findRetryableEvents()`
- `OrderWebhookEvent.markProcessing()`
- `OrderService.upsertFromWebhook()`
- `Cafe24OrderClient.getOrder()`
- `OrderService.upsert()`
- `OrderWebhookEvent.markProcessed()`
- `OrderWebhookEvent.markFailed()`
- `OrderWebhookEvent.markDead()`

### 8단계: 주문 정기 동기화 흐름

- `OrderSyncScheduler.syncOrders()`
- `OrderService.syncFromCafe24()`
- `Cafe24OrderClient.getOrders()`
- `OrderService.syncPages()`
- `OrderService.upsert()`
- `SyncMetricsService.recordRun()`

### 9단계: 배송사 흐름

- `CarrierController.register()`
- `CarrierService.registerCarrier()`
- `Cafe24CarrierClient.createCarrier()`
- `CarrierSyncScheduler.syncCarriers()`
- `CarrierService.syncFromCafe24()`
- `Cafe24CarrierClient.getCarriers()`
- `CarrierWebhookController.created()`
- `WebhookEventService.onCarrierCreated()`

### 10단계: JPA 변환 구조 확인

각 도메인별로 같은 패턴을 확인합니다.

```text
Domain Repository Interface
  -> RepositoryAdapter
  -> Mapper
  -> JpaRepository
  -> Entity
```

예:

```text
ProductRepository
  -> ProductRepositoryAdapter
  -> ProductMapper
  -> ProductJpaRepository
  -> ProductEntity
```

이 패턴을 이해하면 `order`, `carrier`, `authorization`, `monitoring`도 같은 방식으로 읽을 수 있습니다.
