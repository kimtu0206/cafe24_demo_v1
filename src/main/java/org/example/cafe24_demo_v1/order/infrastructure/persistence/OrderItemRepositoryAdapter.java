package org.example.cafe24_demo_v1.order.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.order.domain.model.OrderItem;
import org.example.cafe24_demo_v1.order.domain.repository.OrderItemRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderItemRepositoryAdapter implements OrderItemRepository {

    private final OrderItemJpaRepository jpaRepository;
    private final OrderItemMapper mapper;

    @Override
    public void deleteByMallIdAndCafe24OrderId(String mallId, String cafe24OrderId) {
        jpaRepository.deleteByMallIdAndCafe24OrderId(mallId, cafe24OrderId);
    }

    @Override
    public void saveAll(List<OrderItem> items) {
        for (OrderItem item : items) {
            OrderItemEntity saved = jpaRepository.save(mapper.toEntity(item));
            item.setId(saved.getId());
        }
    }
}
