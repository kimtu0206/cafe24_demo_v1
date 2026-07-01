package org.example.cafe24_demo_v1.order.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.order.domain.model.OrderBuyer;
import org.example.cafe24_demo_v1.order.domain.repository.OrderBuyerRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OrderBuyerRepositoryAdapter implements OrderBuyerRepository {

    private final OrderBuyerJpaRepository jpaRepository;
    private final OrderBuyerMapper mapper;

    @Override
    public void deleteByMallIdAndCafe24OrderId(String mallId, String cafe24OrderId) {
        jpaRepository.deleteByMallIdAndCafe24OrderId(mallId, cafe24OrderId);
    }

    @Override
    public void save(OrderBuyer buyer) {
        OrderBuyerEntity saved = jpaRepository.save(mapper.toEntity(buyer));
        buyer.setId(saved.getId());
    }
}
