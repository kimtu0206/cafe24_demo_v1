package org.example.cafe24_demo_v1.member.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class Member {
    private final Integer shopNo;
    private final String memberId;
    private final String memberName;
    private final Integer groupNo;
    private final String memberAuthentication;
    private final String useBlacklist;
    private final String blacklistType;
    private final String authenticationMethod;
    private final String sms;
    private final String newsMail;
    private final String solarCalendar;
    private final String totalPoints;
    private final String availablePoints;
    private final String usedPoints;
    private final String useMobileApp;
    private final String availableCredits;
    private final String fixedGroup;
    private final String gender;
    private final String email;
    private final String cellphone;
    private final String phone;
    private final String birthday;
    private final LocalDateTime createdDate;
    private final LocalDateTime lastLoginDate;
}
