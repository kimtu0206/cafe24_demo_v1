package org.example.cafe24_demo_v1.member.infrastructure.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * Cafe24 Admin 회원 API가 반환하는 "customer" 객체를 역직렬화하는 DTO.
 * infrastructure 레이어 내부에서만 사용한다.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class Cafe24CustomerPayload {

    @JsonProperty("shop_no")
    private Integer shopNo;

    @JsonProperty("member_id")
    private String memberId;

    @JsonProperty("member_name")
    private String memberName;

    @JsonProperty("group_no")
    private Integer groupNo;

    @JsonProperty("member_authentication")
    private String memberAuthentication;

    @JsonProperty("use_blacklist")
    private String useBlacklist;

    @JsonProperty("blacklist_type")
    private String blacklistType;

    @JsonProperty("authentication_method")
    private String authenticationMethod;

    private String sms;

    @JsonProperty("news_mail")
    private String newsMail;

    @JsonProperty("solar_calendar")
    private String solarCalendar;

    @JsonProperty("total_points")
    private String totalPoints;

    @JsonProperty("available_points")
    private String availablePoints;

    @JsonProperty("used_points")
    private String usedPoints;

    @JsonProperty("use_mobile_app")
    private String useMobileApp;

    @JsonProperty("available_credits")
    private String availableCredits;

    @JsonProperty("fixed_group")
    private String fixedGroup;

    private String gender;

    private String email;

    private String cellphone;

    private String phone;

    private String birthday;

    @JsonProperty("created_date")
    private OffsetDateTime createdDate;

    @JsonProperty("last_login_date")
    private OffsetDateTime lastLoginDate;
}
