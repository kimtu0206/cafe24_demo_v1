package org.example.cafe24_demo_v1.member.application.service;

import org.example.cafe24_demo_v1.authorization.application.service.AppAuthorizationService;
import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.member.domain.model.Member;
import org.example.cafe24_demo_v1.member.domain.service.Cafe24MemberPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock private Cafe24MemberPort cafe24MemberPort;
    @Mock private AppAuthorizationService authorizationService;

    private MemberService memberService;

    private final TokenCredential credential = new TokenCredential(
            "access-token", "refresh-token", "Bearer",
            LocalDateTime.now().plusHours(1), LocalDateTime.now().plusDays(1)
    );

    @BeforeEach
    void setUp() {
        memberService = new MemberService(cafe24MemberPort, authorizationService);
    }

    @Test
    void member_id로_검색하면_port의_searchMembers에_위임한다() {
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);
        Member member = new Member(1, "testid", "홍길동", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        given(cafe24MemberPort.searchMembers("mymall", "testid", null, credential)).willReturn(List.of(member));

        List<Member> result = memberService.search("mymall", "testid", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMemberId()).isEqualTo("testid");
    }

    @Test
    void cellphone으로_검색하면_port의_searchMembers에_위임한다() {
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);
        given(cafe24MemberPort.searchMembers("mymall", null, "01012345678", credential)).willReturn(List.of());

        List<Member> result = memberService.search("mymall", null, "01012345678");

        assertThat(result).isEmpty();
        verify(cafe24MemberPort).searchMembers("mymall", null, "01012345678", credential);
    }

    @Test
    void getValidCredential을_먼저_호출하고_port에_위임한다() {
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);
        given(cafe24MemberPort.searchMembers(any(), any(), any(), any())).willReturn(List.of());

        memberService.search("mymall", "testid", null);

        InOrder inOrder = Mockito.inOrder(authorizationService, cafe24MemberPort);
        inOrder.verify(authorizationService).getValidCredential("mymall");
        inOrder.verify(cafe24MemberPort).searchMembers(any(), any(), any(), any());
    }
}
