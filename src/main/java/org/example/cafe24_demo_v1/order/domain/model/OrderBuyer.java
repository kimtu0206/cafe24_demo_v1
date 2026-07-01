package org.example.cafe24_demo_v1.order.domain.model;

import lombok.Getter;

@Getter
public class OrderBuyer {

    private Long id;
    private Long orderFkId;
    private String mallId;
    private String cafe24OrderId;
    private Integer shopNo;
    private String memberId;
    private Integer memberGroupNo;
    private String name;
    private String namesFurigana;
    private String email;
    private String phone;
    private String cellphone;
    private String customerNotification;
    private String updatedDate;
    private String userId;
    private String userName;
    private String companyName;
    private String companyRegistrationNo;
    private String buyerZipcode;
    private String buyerAddress1;
    private String buyerAddress2;
    private String rawJson;

    private OrderBuyer() {}

    public static OrderBuyer of(
            Long orderFkId,
            String mallId,
            String cafe24OrderId,
            Integer shopNo,
            String memberId,
            Integer memberGroupNo,
            String name,
            String namesFurigana,
            String email,
            String phone,
            String cellphone,
            String customerNotification,
            String updatedDate,
            String userId,
            String userName,
            String companyName,
            String companyRegistrationNo,
            String buyerZipcode,
            String buyerAddress1,
            String buyerAddress2,
            String rawJson
    ) {
        OrderBuyer buyer = new OrderBuyer();
        buyer.orderFkId = orderFkId;
        buyer.mallId = mallId;
        buyer.cafe24OrderId = cafe24OrderId;
        buyer.shopNo = shopNo;
        buyer.memberId = memberId;
        buyer.memberGroupNo = memberGroupNo;
        buyer.name = name;
        buyer.namesFurigana = namesFurigana;
        buyer.email = email;
        buyer.phone = phone;
        buyer.cellphone = cellphone;
        buyer.customerNotification = customerNotification;
        buyer.updatedDate = updatedDate;
        buyer.userId = userId;
        buyer.userName = userName;
        buyer.companyName = companyName;
        buyer.companyRegistrationNo = companyRegistrationNo;
        buyer.buyerZipcode = buyerZipcode;
        buyer.buyerAddress1 = buyerAddress1;
        buyer.buyerAddress2 = buyerAddress2;
        buyer.rawJson = rawJson;
        return buyer;
    }

    public void setId(Long id) { this.id = id; }
}
