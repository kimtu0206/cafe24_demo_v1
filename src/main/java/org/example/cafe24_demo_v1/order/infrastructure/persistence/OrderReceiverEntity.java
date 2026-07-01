package org.example.cafe24_demo_v1.order.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "cafe24_order_receiver")
@Getter
@Setter
class OrderReceiverEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_fk_id", nullable = false)
    private Long orderFkId;

    @Column(name = "mall_id", nullable = false)
    private String mallId;

    @Column(name = "cafe24_order_id", nullable = false)
    private String cafe24OrderId;

    @Column(name = "shop_no")
    private Integer shopNo;

    @Column(name = "name")
    private String name;

    @Column(name = "name_furigana")
    private String nameFurigana;

    @Column(name = "phone")
    private String phone;

    @Column(name = "cellphone")
    private String cellphone;

    @Column(name = "virtual_phone_no")
    private String virtualPhoneNo;

    @Column(name = "zipcode")
    private String zipcode;

    @Column(name = "address1")
    private String address1;

    @Column(name = "address2")
    private String address2;

    @Column(name = "address_state")
    private String addressState;

    @Column(name = "address_city")
    private String addressCity;

    @Column(name = "address_street")
    private String addressStreet;

    @Column(name = "address_full", columnDefinition = "TEXT")
    private String addressFull;

    @Column(name = "name_en")
    private String nameEn;

    @Column(name = "city_en")
    private String cityEn;

    @Column(name = "state_en")
    private String stateEn;

    @Column(name = "street_en")
    private String streetEn;

    @Column(name = "country_code")
    private String countryCode;

    @Column(name = "country_name")
    private String countryName;

    @Column(name = "country_name_en")
    private String countryNameEn;

    @Column(name = "shipping_message", columnDefinition = "TEXT")
    private String shippingMessage;

    @Column(name = "clearance_information_type")
    private String clearanceInformationType;

    @Column(name = "clearance_information", columnDefinition = "TEXT")
    private String clearanceInformation;

    @Column(name = "wished_delivery_date")
    private String wishedDeliveryDate;

    @Column(name = "wished_delivery_time")
    private String wishedDeliveryTime;

    @Column(name = "shipping_code")
    private String shippingCode;

    @Column(name = "raw_json", columnDefinition = "TEXT")
    private String rawJson;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
