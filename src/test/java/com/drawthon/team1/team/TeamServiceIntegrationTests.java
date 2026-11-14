package com.drawthon.team1.team;

import com.drawthon.team1.entity.Member;
import com.drawthon.team1.sessionlogin.repository.MemberRepository;
import com.drawthon.team1.team.service.TeamService;
import com.drawthon.team1.team.web.dto.TeamResponses;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class TeamServiceIntegrationTests {

    @Autowired
    private TeamService teamService;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void createInviteAcceptAndQueryTeamFlow() {
        Member owner = createMember("owner");
        Member guest = createMember("guest");

        TeamResponses.TeamDetail created = teamService.createTeam(owner.getId(), " 드로우톤 1팀 ");
        assertEquals("드로우톤 1팀", created.name());
        assertEquals(1, created.memberCount());

        TeamResponses.Invitation invitation = teamService.createInvitation(owner.getId(), created.id());
        assertTrue(invitation.inviteUrl().contains(invitation.token()));

        TeamResponses.InvitationPreview preview = teamService.getInvitationPreview(
                guest.getId(), invitation.token());
        assertFalse(preview.alreadyJoined());
        assertEquals(1, preview.memberCount());

        TeamResponses.JoinResult joinResult = teamService.acceptInvitation(guest.getId(), invitation.token());
        assertTrue(joinResult.joined());

        TeamResponses.JoinResult duplicateJoin = teamService.acceptInvitation(guest.getId(), invitation.token());
        assertFalse(duplicateJoin.joined());

        TeamResponses.TeamDetail team = teamService.getTeam(guest.getId(), created.id());
        assertEquals(2, team.memberCount());
        assertEquals(1, teamService.getMyTeams(guest.getId()).size());
    }

    private Member createMember(String prefix) {
        String uniqueEmail = prefix + "+" + UUID.randomUUID() + "@example.com";
        return memberRepository.save(Member.create(uniqueEmail, "test-password-hash", prefix));
    }
}
