package org.example.cafe24_demo_v1.member.domain.service;

import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.member.domain.model.Member;

import java.util.List;

/**
 * Cafe24 Admin 회원 API와 통신하는 외부 포트(Port) 인터페이스.
 *
 * 도메인 레이어에 위치하지만 구현체는 infrastructure 레이어의 Cafe24MemberClient가 담당한다.
 */
public interface Cafe24MemberPort {

    /**
     * 회원아이디 또는 휴대전화로 회원 목록을 검색한다.
     * memberIds, cellphone 중 최소 하나는 null이 아니어야 한다.
     */
    List<Member> searchMembers(String mallId, String memberIds, String cellphone, TokenCredential credential);
}
