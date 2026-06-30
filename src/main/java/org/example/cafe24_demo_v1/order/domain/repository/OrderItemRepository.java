package org.example.cafe24_demo_v1.order.domain.repository;

import org.example.cafe24_demo_v1.order.domain.model.OrderItem;

import java.util.List;

public interface OrderItemRepository {

    void deleteByMallIdAndCafe24OrderId(String mallId, String cafe24OrderId);

    void saveAll(List<OrderItem> items);
}
