package org.example.cafe24_demo_v1.order.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.order.domain.model.OrderWebhookEvent;
import org.example.cafe24_demo_v1.order.domain.model.OrderWebhookEventStatus;
import org.example.cafe24_demo_v1.order.domain.repository.OrderWebhookEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class OrderWebhookEventRepositoryAdapter implements OrderWebhookEventRepository {

    private final OrderWebhookEventJpaRepository jpaRepository;
    private final OrderWebhookEventMapper mapper;

    @Override
    public Optional<OrderWebhookEvent> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

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
    public List<OrderWebhookEvent> findRetryableEvents(LocalDateTime now, LocalDateTime processingStaleBefore, int limit) {
        return jpaRepository.findRetryable(now, processingStaleBefore, PageRequest.of(0, limit)).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countByStatus(OrderWebhookEventStatus status) {
        return jpaRepository.countByStatus(status.name());
    }

    @Override
    public long sumRetryCount() {
        return jpaRepository.sumRetryCount();
    }
}
