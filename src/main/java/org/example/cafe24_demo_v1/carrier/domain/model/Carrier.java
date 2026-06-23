package org.example.cafe24_demo_v1.carrier.domain.model;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Cafe24에 등록된 배송사를 나타내는 도메인 모델.
 *
 * 배송사 데이터의 원천은 항상 Cafe24이며, 이 모델은 Cafe24 응답을 로컬 DB에 보관하기 위한
 * 표현이다. 외부에서 필드를 직접 수정하지 못하도록 setter를 열지 않는다.
 */
@Getter
public class Carrier {

    private Long id;                       // DB PK (영속화 후 채워짐)
    private Long carrierId;                // Cafe24 배송사 번호
    private String mallId;                 // 배송사가 등록된 쇼핑몰 ID
    private String shippingCarrierCode;    // Cafe24 택배사 코드 (등록 후 불변)
    private String shippingCarrierName;
    private String contact;
    private String secondaryContact;
    private String email;
    private String trackShipmentUrl;
    private BigDecimal defaultShippingFee;
    private String homepageUrl;
    private ShippingType shippingType;
    private boolean defaultCarrier;
    private boolean shippingFeeSetting;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Carrier() {}

    /** Cafe24 배송사 등록/조회 응답을 받아 신규 로컬 배송사를 생성할 때 사용한다. */
    public static Carrier register(
            String mallId,
            Long carrierId,
            String shippingCarrierCode,
            String shippingCarrierName,
            String contact,
            String secondaryContact,
            String email,
            String trackShipmentUrl,
            BigDecimal defaultShippingFee,
            String homepageUrl,
            ShippingType shippingType,
            boolean defaultCarrier,
            boolean shippingFeeSetting
    ) {
        Objects.requireNonNull(shippingCarrierName);
        Objects.requireNonNull(shippingType);
        Carrier carrier = new Carrier();
        carrier.mallId = mallId;
        carrier.carrierId = carrierId;
        carrier.shippingCarrierCode = shippingCarrierCode;
        carrier.shippingCarrierName = shippingCarrierName;
        carrier.contact = contact;
        carrier.secondaryContact = secondaryContact;
        carrier.email = email;
        carrier.trackShipmentUrl = trackShipmentUrl;
        carrier.defaultShippingFee = defaultShippingFee;
        carrier.homepageUrl = homepageUrl;
        carrier.shippingType = shippingType;
        carrier.defaultCarrier = defaultCarrier;
        carrier.shippingFeeSetting = shippingFeeSetting;
        carrier.createdAt = LocalDateTime.now();
        carrier.updatedAt = LocalDateTime.now();
        return carrier;
    }

    /** DB에서 조회한 데이터로 도메인 객체를 복원할 때 사용한다. */
    public static Carrier reconstitute(
            Long id,
            Long carrierId,
            String mallId,
            String shippingCarrierCode,
            String shippingCarrierName,
            String contact,
            String secondaryContact,
            String email,
            String trackShipmentUrl,
            BigDecimal defaultShippingFee,
            String homepageUrl,
            ShippingType shippingType,
            boolean defaultCarrier,
            boolean shippingFeeSetting,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        Carrier carrier = new Carrier();
        carrier.id = id;
        carrier.carrierId = carrierId;
        carrier.mallId = mallId;
        carrier.shippingCarrierCode = shippingCarrierCode;
        carrier.shippingCarrierName = shippingCarrierName;
        carrier.contact = contact;
        carrier.secondaryContact = secondaryContact;
        carrier.email = email;
        carrier.trackShipmentUrl = trackShipmentUrl;
        carrier.defaultShippingFee = defaultShippingFee;
        carrier.homepageUrl = homepageUrl;
        carrier.shippingType = shippingType;
        carrier.defaultCarrier = defaultCarrier;
        carrier.shippingFeeSetting = shippingFeeSetting;
        carrier.createdAt = createdAt;
        carrier.updatedAt = updatedAt;
        return carrier;
    }

    /**
     * Cafe24로부터 받은 최신 정보로 배송사 정보를 갱신한다.
     * shippingCarrierCode는 Cafe24 PUT으로도 변경할 수 없는 필드이므로 갱신 대상에서 제외한다.
     */
    public void applySnapshot(
            String shippingCarrierName,
            String contact,
            String secondaryContact,
            String email,
            String trackShipmentUrl,
            BigDecimal defaultShippingFee,
            String homepageUrl,
            ShippingType shippingType,
            boolean defaultCarrier,
            boolean shippingFeeSetting
    ) {
        Objects.requireNonNull(shippingCarrierName);
        Objects.requireNonNull(shippingType);
        this.shippingCarrierName = shippingCarrierName;
        this.contact = contact;
        this.secondaryContact = secondaryContact;
        this.email = email;
        this.trackShipmentUrl = trackShipmentUrl;
        this.defaultShippingFee = defaultShippingFee;
        this.homepageUrl = homepageUrl;
        this.shippingType = shippingType;
        this.defaultCarrier = defaultCarrier;
        this.shippingFeeSetting = shippingFeeSetting;
        this.updatedAt = LocalDateTime.now();
    }

    // 패키지 내부에서만 호출 가능 — DB 저장 후 생성된 PK를 주입할 때 사용
    public void setId(Long id) { this.id = id; }
}
