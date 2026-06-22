package org.example.cafe24_demo_v1.order.domain.model;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Cafe24에 등록된 주문을 나타내는 도메인 모델.
 *
 * 주문의 원천 데이터는 항상 Cafe24이며, 이 모델은 Cafe24 응답(목록 조회/Webhook)을
 * 로컬 DB에 보관하기 위한 표현이다. 외부에서 필드를 직접 수정하지 못하도록 setter를 열지 않는다.
 */
@Getter
public class Order {

    private Long id;                  // DB PK (영속화 후 채워짐)
    private String mallId;            // 주문이 발생한 쇼핑몰 ID
    private String orderId;           // Cafe24 주문번호 (예: 20200717-0029236)
    private String orderStatus;
    private String memberId;
    private String buyerName;
    private String buyerEmail;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private LocalDateTime orderedAt;
    private String rawJson;           // Cafe24 응답 원본(JSON). 컬럼화하지 않은 나머지 정보를 보존한다.
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Order() {}

    /** Cafe24 주문 조회 응답을 받아 신규 로컬 주문을 생성할 때 사용한다. */
    public static Order register(
            String mallId,
            String orderId,
            String orderStatus,
            String memberId,
            String buyerName,
            String buyerEmail,
            BigDecimal totalAmount,
            String paymentMethod,
            LocalDateTime orderedAt,
            String rawJson
    ) {
        Objects.requireNonNull(mallId, "mallId must not be null");
        Objects.requireNonNull(orderId, "orderId must not be null");

        Order order = new Order();
        order.mallId = mallId;
        order.orderId = orderId;
        order.orderStatus = orderStatus;
        order.memberId = memberId;
        order.buyerName = buyerName;
        order.buyerEmail = buyerEmail;
        order.totalAmount = totalAmount;
        order.paymentMethod = paymentMethod;
        order.orderedAt = orderedAt;
        order.rawJson = rawJson;
        order.createdAt = LocalDateTime.now();
        order.updatedAt = LocalDateTime.now();
        return order;
    }

    /** DB에서 조회한 데이터로 도메인 객체를 복원할 때 사용한다. */
    public static Order reconstitute(
            Long id,
            String mallId,
            String orderId,
            String orderStatus,
            String memberId,
            String buyerName,
            String buyerEmail,
            BigDecimal totalAmount,
            String paymentMethod,
            LocalDateTime orderedAt,
            String rawJson,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        Order order = new Order();
        order.id = id;
        order.mallId = mallId;
        order.orderId = orderId;
        order.orderStatus = orderStatus;
        order.memberId = memberId;
        order.buyerName = buyerName;
        order.buyerEmail = buyerEmail;
        order.totalAmount = totalAmount;
        order.paymentMethod = paymentMethod;
        order.orderedAt = orderedAt;
        order.rawJson = rawJson;
        order.createdAt = createdAt;
        order.updatedAt = updatedAt;
        return order;
    }

    /**
     * Cafe24로부터 받은 최신 정보로 주문 정보를 갱신한다.
     * 증분동기화 스케줄러, Webhook 처리 시 사용한다.
     */
    public void applySnapshot(
            String orderStatus,
            String memberId,
            String buyerName,
            String buyerEmail,
            BigDecimal totalAmount,
            String paymentMethod,
            LocalDateTime orderedAt,
            String rawJson
    ) {
        this.orderStatus = orderStatus;
        this.memberId = memberId;
        this.buyerName = buyerName;
        this.buyerEmail = buyerEmail;
        this.totalAmount = totalAmount;
        this.paymentMethod = paymentMethod;
        this.orderedAt = orderedAt;
        this.rawJson = rawJson;
        this.updatedAt = LocalDateTime.now();
    }

    // DB 저장 후 생성된 PK를 주입할 때 사용
    public void setId(Long id) { this.id = id; }
}
