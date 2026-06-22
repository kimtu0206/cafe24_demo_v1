package org.example.cafe24_demo_v1.order.domain.repository;

import org.example.cafe24_demo_v1.order.domain.model.Order;

import java.util.Optional;

/**
 * 주문(Order) 저장소 포트(인터페이스).
 *
 * 도메인 레이어에 위치하기 때문에 JPA나 DB에 대한 의존성이 전혀 없다.
 * 실제 구현체(JPA)는 infrastructure 레이어의 OrderRepositoryAdapter가 담당한다.
 */
public interface OrderRepository {

    Optional<Order> findByMallIdAndOrderId(String mallId, String orderId);

    void save(Order order);
}