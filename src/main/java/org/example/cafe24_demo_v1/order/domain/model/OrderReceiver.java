package org.example.cafe24_demo_v1.order.domain.model;

import lombok.Getter;

@Getter
public class OrderReceiver {

    private Long id;
    private Long orderFkId;
    private String mallId;
    private String cafe24OrderId;
    private Integer shopNo;
    private String name;
    private String nameFurigana;
    private String phone;
    private String cellphone;
    private String virtualPhoneNo;
    private String zipcode;
    private String address1;
    private String address2;
    private String addressState;
    private String addressCity;
    private String addressStreet;
    private String addressFull;
    private String nameEn;
    private String cityEn;
    private String stateEn;
    private String streetEn;
    private String countryCode;
    private String countryName;
    private String countryNameEn;
    private String shippingMessage;
    private String clearanceInformationType;
    private String clearanceInformation;
    private String wishedDeliveryDate;
    private String wishedDeliveryTime;
    private String shippingCode;
    private String rawJson;

    private OrderReceiver() {}

    public static OrderReceiver of(
            Long orderFkId,
            String mallId,
            String cafe24OrderId,
            Integer shopNo,
            String name,
            String nameFurigana,
            String phone,
            String cellphone,
            String virtualPhoneNo,
            String zipcode,
            String address1,
            String address2,
            String addressState,
            String addressCity,
            String addressStreet,
            String addressFull,
            String nameEn,
            String cityEn,
            String stateEn,
            String streetEn,
            String countryCode,
            String countryName,
            String countryNameEn,
            String shippingMessage,
            String clearanceInformationType,
            String clearanceInformation,
            String wishedDeliveryDate,
            String wishedDeliveryTime,
            String shippingCode,
            String rawJson
    ) {
        OrderReceiver r = new OrderReceiver();
        r.orderFkId = orderFkId;
        r.mallId = mallId;
        r.cafe24OrderId = cafe24OrderId;
        r.shopNo = shopNo;
        r.name = name;
        r.nameFurigana = nameFurigana;
        r.phone = phone;
        r.cellphone = cellphone;
        r.virtualPhoneNo = virtualPhoneNo;
        r.zipcode = zipcode;
        r.address1 = address1;
        r.address2 = address2;
        r.addressState = addressState;
        r.addressCity = addressCity;
        r.addressStreet = addressStreet;
        r.addressFull = addressFull;
        r.nameEn = nameEn;
        r.cityEn = cityEn;
        r.stateEn = stateEn;
        r.streetEn = streetEn;
        r.countryCode = countryCode;
        r.countryName = countryName;
        r.countryNameEn = countryNameEn;
        r.shippingMessage = shippingMessage;
        r.clearanceInformationType = clearanceInformationType;
        r.clearanceInformation = clearanceInformation;
        r.wishedDeliveryDate = wishedDeliveryDate;
        r.wishedDeliveryTime = wishedDeliveryTime;
        r.shippingCode = shippingCode;
        r.rawJson = rawJson;
        return r;
    }

    public void setId(Long id) { this.id = id; }
}
