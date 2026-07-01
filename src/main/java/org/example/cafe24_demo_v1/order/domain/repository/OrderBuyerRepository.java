package org.example.cafe24_demo_v1.order.domain.repository;

import org.example.cafe24_demo_v1.order.domain.model.OrderBuyer;

public interface OrderBuyerRepository {

    void deleteByMallIdAndCafe24OrderId(String mallId, String cafe24OrderId);

    void save(OrderBuyer buyer);
}
