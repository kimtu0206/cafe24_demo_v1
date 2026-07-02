package org.example.cafe24_demo_v1.order.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.cafe24_demo_v1.order.domain.model.Order;
import org.example.cafe24_demo_v1.order.domain.model.OrderBuyer;
import org.example.cafe24_demo_v1.order.domain.model.OrderEmbeddedResources;
import org.example.cafe24_demo_v1.order.domain.model.OrderReceiver;
import org.example.cafe24_demo_v1.order.domain.repository.OrderBuyerRepository;
import org.example.cafe24_demo_v1.order.domain.repository.OrderItemRepository;
import org.example.cafe24_demo_v1.order.domain.repository.OrderReceiverRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * OrderRepositoryAdapter.save()가 order/buyer/receiver 테이블에 걸쳐 수행하는
 * delete-then-insert 동기화를 검증한다. Mapper들은 실제 파싱 로직을 태우기 위해
 * 실 인스턴스를 사용하고, JPA/도메인 저장소 포트만 mock으로 대체한다.
 */
@ExtendWith(MockitoExtension.class)
class OrderRepositoryAdapterTest {

    @Mock private OrderJpaRepository jpaRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private OrderReceiverRepository orderReceiverRepository;
    @Mock private OrderBuyerRepository orderBuyerRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OrderMapper orderMapper = new OrderMapper();
    private final OrderItemMapper orderItemMapper = new OrderItemMapper(objectMapper);
    private final OrderReceiverMapper orderReceiverMapper = new OrderReceiverMapper(objectMapper);
    private final OrderBuyerMapper orderBuyerMapper = new OrderBuyerMapper(objectMapper);

    private OrderRepositoryAdapter adapter;

    private static final String MEMBER_BUYER_JSON = """
            {"shop_no":1,"member_id":"member1","name":"홍길동","email":"member@test.com"}
            """;

    private static final String GUEST_BUYER_JSON = """
            {"shop_no":1,"member_id":null,"name":"비회원","email":"guest@test.com","cellphone":"010-1111-2222"}
            """;

    private static final String RECEIVERS_JSON = """
            [{"shop_no":1,"name":"수령인","address1":"서울시 강남구"}]
            """;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        adapter = new OrderRepositoryAdapter(
                jpaRepository, orderMapper,
                orderItemRepository, orderItemMapper,
                orderReceiverRepository, orderReceiverMapper,
                orderBuyerRepository, orderBuyerMapper
        );
        given(jpaRepository.save(any(OrderEntity.class))).willAnswer(invocation -> {
            OrderEntity entity = invocation.getArgument(0);
            entity.setId(100L);
            return entity;
        });
    }

    @Test
    void save는_buyer_JSON이_있으면_기존_buyer를_삭제하고_새로_저장한다() {
        Order order = orderWithBuyer("mymall", "order-1", MEMBER_BUYER_JSON);

        adapter.save(order);

        verify(orderBuyerRepository).deleteByMallIdAndCafe24OrderId("mymall", "order-1");
        ArgumentCaptor<OrderBuyer> captor = ArgumentCaptor.forClass(OrderBuyer.class);
        verify(orderBuyerRepository).save(captor.capture());
        assertThat(captor.getValue().getMemberId()).isEqualTo("member1");
        assertThat(captor.getValue().getOrderFkId()).isEqualTo(100L);
    }

    @Test
    void save는_비회원_buyer_JSON도_member_id_null인_채로_정상_저장한다() {
        Order order = orderWithBuyer("mymall", "order-2", GUEST_BUYER_JSON);

        adapter.save(order);

        ArgumentCaptor<OrderBuyer> captor = ArgumentCaptor.forClass(OrderBuyer.class);
        verify(orderBuyerRepository).save(captor.capture());
        assertThat(captor.getValue().getMemberId()).isNull();
        assertThat(captor.getValue().getName()).isEqualTo("비회원");
        assertThat(captor.getValue().getEmail()).isEqualTo("guest@test.com");
    }

    @Test
    void save는_buyer_JSON이_없으면_buyer_저장소를_건드리지_않는다() {
        Order order = orderWithBuyer("mymall", "order-3", null);

        adapter.save(order);

        verifyNoInteractions(orderBuyerRepository);
    }

    @Test
    void save는_buyer_JSON이_배열이면_삭제만_하고_저장은_하지_않는다() {
        Order order = orderWithBuyer("mymall", "order-4", "[{\"shop_no\":1}]");

        adapter.save(order);

        verify(orderBuyerRepository).deleteByMallIdAndCafe24OrderId("mymall", "order-4");
        verify(orderBuyerRepository, never()).save(any());
    }

    @Test
    void save는_receivers_JSON이_있으면_기존_receiver를_삭제하고_새로_저장한다() {
        Order order = orderWithReceivers("mymall", "order-5", RECEIVERS_JSON);

        adapter.save(order);

        verify(orderReceiverRepository).deleteByMallIdAndCafe24OrderId("mymall", "order-5");
        ArgumentCaptor<java.util.List<OrderReceiver>> captor = ArgumentCaptor.forClass(java.util.List.class);
        verify(orderReceiverRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getName()).isEqualTo("수령인");
    }

    @Test
    void save는_receivers_JSON이_없으면_receiver_저장소를_건드리지_않는다() {
        Order order = orderWithReceivers("mymall", "order-6", null);

        adapter.save(order);

        verifyNoInteractions(orderReceiverRepository);
    }

    private Order orderWithBuyer(String mallId, String orderId, String buyerJson) {
        return Order.register(
                mallId, orderId, "N10", null, null, null,
                new BigDecimal("1000"), "card", LocalDateTime.now(), "{}", null, null,
                new OrderEmbeddedResources(null, null, buyerJson, null, null, null, null, null, null)
        );
    }

    private Order orderWithReceivers(String mallId, String orderId, String receiversJson) {
        return Order.register(
                mallId, orderId, "N10", null, null, null,
                new BigDecimal("1000"), "card", LocalDateTime.now(), "{}", null, null,
                new OrderEmbeddedResources(null, receiversJson, null, null, null, null, null, null, null)
        );
    }
}
