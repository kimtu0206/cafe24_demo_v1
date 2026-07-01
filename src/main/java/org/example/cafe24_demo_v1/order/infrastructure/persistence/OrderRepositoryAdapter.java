package org.example.cafe24_demo_v1.order.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.order.domain.model.Order;
import org.example.cafe24_demo_v1.order.domain.model.OrderItem;
import org.example.cafe24_demo_v1.order.domain.model.OrderReceiver;
import org.example.cafe24_demo_v1.order.domain.repository.OrderBuyerRepository;
import org.example.cafe24_demo_v1.order.domain.repository.OrderItemRepository;
import org.example.cafe24_demo_v1.order.domain.repository.OrderReceiverRepository;
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
    private final OrderReceiverRepository orderReceiverRepository;
    private final OrderReceiverMapper orderReceiverMapper;
    private final OrderBuyerRepository orderBuyerRepository;
    private final OrderBuyerMapper orderBuyerMapper;

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
        syncReceivers(order, saved.getId());
        syncBuyer(order, saved.getId());
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

    private void syncReceivers(Order order, Long orderFkId) {
        String receiversJson = order.getReceivers();
        if (receiversJson == null) {
            return;
        }
        orderReceiverRepository.deleteByMallIdAndCafe24OrderId(order.getMallId(), order.getOrderId());
        List<OrderReceiver> receivers = orderReceiverMapper.fromReceiversJson(orderFkId, order.getMallId(), order.getOrderId(), receiversJson);
        orderReceiverRepository.saveAll(receivers);
    }

    private void syncBuyer(Order order, Long orderFkId) {
        String buyerJson = order.getBuyer();
        if (buyerJson == null) {
            return;
        }
        orderBuyerRepository.deleteByMallIdAndCafe24OrderId(order.getMallId(), order.getOrderId());
        orderBuyerMapper.fromBuyerJson(orderFkId, order.getMallId(), order.getOrderId(), buyerJson)
                .ifPresent(orderBuyerRepository::save);
    }
}
