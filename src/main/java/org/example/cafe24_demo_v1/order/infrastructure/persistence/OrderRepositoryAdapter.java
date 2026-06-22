package org.example.cafe24_demo_v1.order.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.order.domain.model.Order;
import org.example.cafe24_demo_v1.order.domain.repository.OrderRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository jpaRepository;
    private final OrderMapper mapper;

    @Override
    public Optional<Order> findByMallIdAndOrderId(String mallId, String orderId) {
        return jpaRepository.findByMallIdAndOrderId(mallId, orderId).map(mapper::toDomain);
    }

    @Override
    public void save(Order order) {
        OrderEntity entity = mapper.toEntity(order);
        OrderEntity saved = jpaRepository.save(entity);
        order.setId(saved.getId());
    }
}
