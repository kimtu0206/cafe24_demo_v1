package org.example.cafe24_demo_v1.order.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.order.domain.model.OrderReceiver;
import org.example.cafe24_demo_v1.order.domain.repository.OrderReceiverRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderReceiverRepositoryAdapter implements OrderReceiverRepository {

    private final OrderReceiverJpaRepository jpaRepository;
    private final OrderReceiverMapper mapper;

    @Override
    public void deleteByMallIdAndCafe24OrderId(String mallId, String cafe24OrderId) {
        jpaRepository.deleteByMallIdAndCafe24OrderId(mallId, cafe24OrderId);
    }

    @Override
    public void saveAll(List<OrderReceiver> receivers) {
        for (OrderReceiver receiver : receivers) {
            OrderReceiverEntity saved = jpaRepository.save(mapper.toEntity(receiver));
            receiver.setId(saved.getId());
        }
    }
}
