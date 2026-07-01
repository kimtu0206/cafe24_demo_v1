package org.example.cafe24_demo_v1.order.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "cafe24_order_buyer")
@Getter
@Setter
class OrderBuyerEntity {

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

    @Column(name = "member_id")
    private String memberId;

    @Column(name = "member_group_no")
    private Integer memberGroupNo;

    @Column(name = "name")
    private String name;

    @Column(name = "names_furigana")
    private String namesFurigana;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "cellphone")
    private String cellphone;

    @Column(name = "customer_notification", columnDefinition = "TEXT")
    private String customerNotification;

    @Column(name = "updated_date")
    private String updatedDate;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "company_registration_no")
    private String companyRegistrationNo;

    @Column(name = "buyer_zipcode")
    private String buyerZipcode;

    @Column(name = "buyer_address1", columnDefinition = "TEXT")
    private String buyerAddress1;

    @Column(name = "buyer_address2", columnDefinition = "TEXT")
    private String buyerAddress2;

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
