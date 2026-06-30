package org.example.cafe24_demo_v1.webhook.domain.model;

/**
 * 수신한 Webhook 이벤트의 종류.
 *
 * Cafe24가 보내는 event_no 값은 머천트가 Cafe24 개발자센터에서 URL별로 직접 등록하는 값이라
 * 코드에서 그 값 자체로 종류를 판별하지 않는다. 대신 어떤 Webhook 엔드포인트/핸들러가 처리했는지로
 * 종류를 결정해 이력 테이블에 사람이 읽을 수 있는 값으로 같이 저장한다.
 */
public enum WebhookEventType {
    APP_UNINSTALLED,
    PRODUCT_CREATED,
    PRODUCT_UPDATED,
    PRODUCT_DELETED,
    CARRIER_CREATED,
    CARRIER_UPDATED,
    CARRIER_DELETED,
    BENEFIT_CREATED,
    BENEFIT_UPDATED
}
