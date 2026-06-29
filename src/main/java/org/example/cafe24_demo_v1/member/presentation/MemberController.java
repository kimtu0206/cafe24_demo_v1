package org.example.cafe24_demo_v1.member.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.member.application.service.MemberService;
import org.example.cafe24_demo_v1.member.domain.model.Member;
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.example.cafe24_demo_v1.shared.exception.Cafe24ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Members", description = "회원 검색 — Cafe24 Admin API 연동 (실패 시 502 반환)")
@Slf4j
@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final Cafe24Properties cafe24Properties;

    @Operation(
            summary = "회원 검색",
            description = "회원아이디(쉼표 구분 복수 가능) 또는 휴대전화로 Cafe24 회원을 검색합니다. 둘 중 하나는 필수입니다."
    )
    @GetMapping
    public ResponseEntity<?> search(
            @Parameter(description = "검색할 회원아이디 (쉼표 구분, 예: sampleid,testid)") @RequestParam(required = false) String memberId,
            @Parameter(description = "검색할 휴대전화번호 (예: 01012345678)") @RequestParam(required = false) String cellphone
    ) {
        if (!StringUtils.hasText(memberId) && !StringUtils.hasText(cellphone)) {
            return ResponseEntity.badRequest().body("member_id 또는 cellphone 중 하나는 필수입니다.");
        }

        List<Member> members = memberService.search(cafe24Properties.getMallId(), memberId, cellphone);
        List<MemberResponse> response = members.stream().map(MemberResponse::from).toList();
        return ResponseEntity.ok(new MemberListResponse(response, response.size()));
    }

    @ExceptionHandler(Cafe24ApiException.class)
    public ResponseEntity<String> handleCafe24ApiException(Cafe24ApiException e) {
        log.error("Cafe24 member API error: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Cafe24 API 호출에 실패했습니다: " + e.getMessage());
    }

    private record MemberResponse(
            Integer shopNo,
            String memberId,
            String memberName,
            Integer groupNo,
            String memberAuthentication,
            String useBlacklist,
            String blacklistType,
            String authenticationMethod,
            String sms,
            String newsMail,
            String solarCalendar,
            String totalPoints,
            String availablePoints,
            String usedPoints,
            String useMobileApp,
            String availableCredits,
            String fixedGroup,
            String gender,
            String email,
            String cellphone,
            String phone,
            String birthday,
            LocalDateTime createdDate,
            LocalDateTime lastLoginDate
    ) {
        static MemberResponse from(Member member) {
            return new MemberResponse(
                    member.getShopNo(),
                    member.getMemberId(),
                    member.getMemberName(),
                    member.getGroupNo(),
                    member.getMemberAuthentication(),
                    member.getUseBlacklist(),
                    member.getBlacklistType(),
                    member.getAuthenticationMethod(),
                    member.getSms(),
                    member.getNewsMail(),
                    member.getSolarCalendar(),
                    member.getTotalPoints(),
                    member.getAvailablePoints(),
                    member.getUsedPoints(),
                    member.getUseMobileApp(),
                    member.getAvailableCredits(),
                    member.getFixedGroup(),
                    member.getGender(),
                    member.getEmail(),
                    member.getCellphone(),
                    member.getPhone(),
                    member.getBirthday(),
                    member.getCreatedDate(),
                    member.getLastLoginDate()
            );
        }
    }

    private record MemberListResponse(List<MemberResponse> members, int count) {}
}
