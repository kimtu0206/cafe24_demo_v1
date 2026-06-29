package org.example.cafe24_demo_v1.benefit.domain.repository;

/**
 * 혜택 Webhook 이벤트 이력(cafe24_benefit_webhook_event) 저장소 포트.
 *
 * 도메인 레이어에 위치하므로 JPA/DB 의존성이 없다.
 * 구현체는 infrastructure 레이어의 BenefitWebhookEventRepositoryAdapter가 담당한다.
 */
public interface BenefitWebhookEventRepository {

    /** eventNo + mallId + resourceId(benefitNo) 조합이 이미 처리된 이벤트인지 확인한다. */
    boolean exists(Integer eventNo, String mallId, String resourceId);

    /** 수신한 이벤트를 이력에 저장한다. eventType은 event_no를 사람이 읽을 수 있게 보조하는 값이다. */
    void save(Integer eventNo, String eventType, String mallId, String resourceId);
}
