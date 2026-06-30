package org.example.cafe24_demo_v1.order.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.order.domain.model.Order;
import org.example.cafe24_demo_v1.order.domain.model.OrderItem;
import org.example.cafe24_demo_v1.order.domain.repository.OrderItemRepository;
import org.example.cafe24_demo_v1.order.domain.repository.OrderRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository jpaRepository;
    private final OrderMapper mapper;
    private final OrderItemRepository orderItemRepository;
    private final OrderItemMapper orderItemMapper;

    @Override
    public Optional<Order> findByMallIdAndOrderId(String mallId, String orderId) {
        return jpaRepository.findByMallIdAndOrderId(mallId, orderId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void save(Order order) {
        OrderEntity entity = mapper.toEntity(order);
        OrderEntity saved = jpaRepository.save(entity);
        order.setId(saved.getId());
        syncItems(order, saved.getId());
    }

    private void syncItems(Order order, Long orderFkId) {
        String itemsJson = order.getItems();
        if (itemsJson == null) {
            return;
        }
        orderItemRepository.deleteByMallIdAndCafe24OrderId(order.getMallId(), order.getOrderId());
        List<OrderItem> items = orderItemMapper.fromItemsJson(orderFkId, order.getMallId(), order.getOrderId(), itemsJson);
        orderItemRepository.saveAll(items);
    }
}
