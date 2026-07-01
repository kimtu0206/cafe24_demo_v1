package org.example.cafe24_demo_v1.order.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

interface OrderReceiverJpaRepository extends JpaRepository<OrderReceiverEntity, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM OrderReceiverEntity r WHERE r.mallId = :mallId AND r.cafe24OrderId = :cafe24OrderId")
    void deleteByMallIdAndCafe24OrderId(@Param("mallId") String mallId, @Param("cafe24OrderId") String cafe24OrderId);
}
