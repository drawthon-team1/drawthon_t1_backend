package com.drawthon.team1.team;

import com.drawthon.team1.entity.Member;
import com.drawthon.team1.entity.MemberRole;
import com.drawthon.team1.sessionlogin.repository.MemberRepository;
import com.drawthon.team1.team.domain.Team;
import com.drawthon.team1.team.domain.TeamInvitation;
import com.drawthon.team1.team.repository.TeamInvitationRepository;
import com.drawthon.team1.team.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TeamApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamInvitationRepository invitationRepository;

    @Test
    void teamMakerTestPageIsPublic() throws Exception {
        mockMvc.perform(get("/teammaker-test.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"loginButton\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("admin1@abc.com")));
    }

    @Test
    void teamApisWorkThroughHttp() throws Exception {
        Member owner = createMember("api-owner");
        Member guest = createMember("api-guest");

        mockMvc.perform(post("/api/teams")
                        .with(authenticatedAs(owner.getId()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"HTTP 테스트팀\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("HTTP 테스트팀"))
                .andExpect(jsonPath("$.memberCount").value(1));

        Team team = teamRepository.findAll().stream()
                .filter(candidate -> candidate.getOwner().getId().equals(owner.getId()))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(post("/api/teams/{teamId}/invitations", team.getId())
                        .with(authenticatedAs(owner.getId()))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.teamId").value(team.getId()))
                .andExpect(jsonPath("$.inviteUrl").isNotEmpty());

        TeamInvitation invitation = invitationRepository.findAll().stream()
                .filter(candidate -> candidate.getTeam().getId().equals(team.getId()))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(get("/api/team-invitations/{token}", invitation.getToken())
                        .with(authenticatedAs(guest.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alreadyJoined").value(false));

        mockMvc.perform(post("/api/team-invitations/{token}/accept", invitation.getToken())
                        .with(authenticatedAs(guest.getId()))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.joined").value(true));

        mockMvc.perform(get("/api/teams/{teamId}", team.getId())
                        .with(authenticatedAs(owner.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberCount").value(2))
                .andExpect(jsonPath("$.members[*].nickname", hasItem(guest.getNickname())));

        mockMvc.perform(get("/api/teams")
                        .with(authenticatedAs(guest.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(team.getId()));

    }

    private Member createMember(String prefix) {
        String email = prefix + "+" + UUID.randomUUID() + "@example.com";
        return memberRepository.save(Member.create(email, "test-password-hash", prefix));
    }

    private RequestPostProcessor authenticatedAs(Long memberId) {
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                memberId,
                null,
                List.of(new SimpleGrantedAuthority(MemberRole.USER.authority())));
        return authentication(authentication);
    }
}
