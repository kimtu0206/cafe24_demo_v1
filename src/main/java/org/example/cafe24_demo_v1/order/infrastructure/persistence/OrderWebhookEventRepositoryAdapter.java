package org.example.cafe24_demo_v1.order.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.order.domain.model.OrderWebhookEvent;
import org.example.cafe24_demo_v1.order.domain.repository.OrderWebhookEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class OrderWebhookEventRepositoryAdapter implements OrderWebhookEventRepository {

    private final OrderWebhookEventJpaRepository jpaRepository;
    private final OrderWebhookEventMapper mapper;

    @Override
    public boolean exists(Integer eventNo, String mallId, String resourceId) {
        return jpaRepository.existsByEventNoAndMallIdAndResourceId(eventNo, mallId, resourceId);
    }

    @Override
    public void save(OrderWebhookEvent event) {
        OrderWebhookEventEntity entity = mapper.toEntity(event);
        OrderWebhookEventEntity saved = jpaRepository.save(entity);
        event.setId(saved.getId());
    }

    @Override
    public List<OrderWebhookEvent> findRetryableEvents(LocalDateTime now, int limit) {
        return jpaRepository.findRetryable(now, PageRequest.of(0, limit)).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
