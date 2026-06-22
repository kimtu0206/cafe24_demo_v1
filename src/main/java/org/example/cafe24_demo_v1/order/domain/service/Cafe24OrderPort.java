package org.example.cafe24_demo_v1.order.domain.service;

import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.order.domain.model.Order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Cafe24 Admin 주문 API와 통신하는 외부 포트(Port) 인터페이스.
 *
 * 도메인 레이어에 위치하지만 구현체는 infrastructure 레이어의 Cafe24OrderClient가 담당한다.
 * 도메인/애플리케이션 레이어는 이 인터페이스만 알고 실제 HTTP 통신 방식은 모른다.
 */
public interface Cafe24OrderPort {

    /** updatedSince(주문 수정일시) 이후 변경된 주문을 offset/limit 페이지네이션으로 조회한다. */
    List<Order> getOrders(String mallId, LocalDateTime updatedSince, int offset, int limit, TokenCredential credential);

    /** orderId로 Cafe24에 등록된 주문 1건을 조회한다. Webhook 처리 시 상세를 다시 조회할 때 사용한다. */
    Optional<Order> getOrder(String mallId, String orderId, TokenCredential credential);
}