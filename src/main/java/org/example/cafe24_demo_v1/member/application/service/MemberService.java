package org.example.cafe24_demo_v1.member.application.service;

import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.authorization.application.service.AppAuthorizationService;
import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.member.domain.model.Member;
import org.example.cafe24_demo_v1.member.domain.service.Cafe24MemberPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final Cafe24MemberPort cafe24MemberPort;
    private final AppAuthorizationService authorizationService;

    /**
     * 회원아이디(쉼표 구분) 또는 휴대전화로 Cafe24 회원을 검색한다.
     * memberIds, cellphone 중 하나는 반드시 제공되어야 한다.
     */
    public List<Member> search(String mallId, String memberIds, String cellphone) {
        TokenCredential credential = authorizationService.getValidCredential(mallId);
        return cafe24MemberPort.searchMembers(mallId, memberIds, cellphone, credential);
    }
}
