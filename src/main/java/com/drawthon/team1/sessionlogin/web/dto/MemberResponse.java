package com.drawthon.team1.sessionlogin.web.dto;

import com.drawthon.team1.entity.Member;
import com.drawthon.team1.entity.MemberRole;

public record MemberResponse(Long id, String email, String nickname, MemberRole role) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getRole());
    }
}
