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
    private OrderType orderType;      // 회원 주문(MEMBER) / 비회원 주문(GUEST)
    private String memberId;
    private String buyerName;
    private String buyerEmail;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private LocalDateTime orderedAt;
    private String rawJson;           // Cafe24 응답 원본(JSON). 컬럼화하지 않은 나머지 정보를 보존한다.
    private String canceled;          // 취소 여부 — Cafe24 "T"/"F"
    private LocalDateTime cancelDate; // 취소 일시 (취소되지 않은 경우 null)
    private String items;             // embed=items 응답 원본(JSON)
    private String receivers;         // embed=receivers 응답 원본(JSON)
    private String buyer;             // embed=buyer 응답 원본(JSON)
    private String returnInfo;        // embed=return 응답 원본(JSON)
    private String cancellation;      // embed=cancellation 응답 원본(JSON)
    private String exchange;          // embed=exchange 응답 원본(JSON)
    private String benefits;          // embed=benefits 응답 원본(JSON)
    private String coupons;           // embed=coupons 응답 원본(JSON)
    private String refunds;           // embed=refunds 응답 원본(JSON)
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
            String rawJson,
            String canceled,
            LocalDateTime cancelDate,
            OrderEmbeddedResources embeds
    ) {
        Objects.requireNonNull(mallId, "mallId must not be null");
        Objects.requireNonNull(orderId, "orderId must not be null");

        Order order = new Order();
        order.mallId = mallId;
        order.orderId = orderId;
        order.orderStatus = orderStatus;
        order.orderType = deriveOrderType(memberId);
        order.memberId = memberId;
        order.buyerName = buyerName;
        order.buyerEmail = buyerEmail;
        order.totalAmount = totalAmount;
        order.paymentMethod = paymentMethod;
        order.orderedAt = orderedAt;
        order.rawJson = rawJson;
        order.canceled = canceled;
        order.cancelDate = cancelDate;
        order.applyEmbeds(embeds);
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
            OrderType orderType,
            String memberId,
            String buyerName,
            String buyerEmail,
            BigDecimal totalAmount,
            String paymentMethod,
            LocalDateTime orderedAt,
            String rawJson,
            String canceled,
            LocalDateTime cancelDate,
            OrderEmbeddedResources embeds,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        Order order = new Order();
        order.id = id;
        order.mallId = mallId;
        order.orderId = orderId;
        order.orderStatus = orderStatus;
        order.orderType = orderType;
        order.memberId = memberId;
        order.buyerName = buyerName;
        order.buyerEmail = buyerEmail;
        order.totalAmount = totalAmount;
        order.paymentMethod = paymentMethod;
        order.orderedAt = orderedAt;
        order.rawJson = rawJson;
        order.canceled = canceled;
        order.cancelDate = cancelDate;
        order.applyEmbeds(embeds);
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
            String rawJson,
            String canceled,
            LocalDateTime cancelDate,
            OrderEmbeddedResources embeds
    ) {
        this.orderStatus = orderStatus;
        this.orderType = deriveOrderType(memberId);
        this.memberId = memberId;
        this.buyerName = buyerName;
        this.buyerEmail = buyerEmail;
        this.totalAmount = totalAmount;
        this.paymentMethod = paymentMethod;
        this.orderedAt = orderedAt;
        this.rawJson = rawJson;
        this.canceled = canceled;
        this.cancelDate = cancelDate;
        applyEmbeds(embeds);
        this.updatedAt = LocalDateTime.now();
    }

    /** 이 주문이 갖고 있는 embed 하위 리소스 원본을 VO로 묶어서 반환한다. */
    public OrderEmbeddedResources getEmbeds() {
        return new OrderEmbeddedResources(
                items, receivers, buyer, returnInfo, cancellation, exchange, benefits, coupons, refunds
        );
    }

    private void applyEmbeds(OrderEmbeddedResources embeds) {
        this.items = embeds.getItems();
        this.receivers = embeds.getReceivers();
        this.buyer = embeds.getBuyer();
        this.returnInfo = embeds.getReturnInfo();
        this.cancellation = embeds.getCancellation();
        this.exchange = embeds.getExchange();
        this.benefits = embeds.getBenefits();
        this.coupons = embeds.getCoupons();
        this.refunds = embeds.getRefunds();
    }

    // DB 저장 후 생성된 PK를 주입할 때 사용
    public void setId(Long id) { this.id = id; }

    private static OrderType deriveOrderType(String memberId) {
        return (memberId != null && !memberId.isBlank()) ? OrderType.MEMBER : OrderType.GUEST;
    }
}
