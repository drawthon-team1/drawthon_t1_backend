package com.drawthon.team1.team.service;

import com.drawthon.team1.entity.Member;
import com.drawthon.team1.sessionlogin.repository.MemberRepository;
import com.drawthon.team1.team.domain.Team;
import com.drawthon.team1.team.domain.TeamInvitation;
import com.drawthon.team1.team.domain.TeamMembership;
import com.drawthon.team1.team.domain.TeamRole;
import com.drawthon.team1.team.repository.TeamInvitationRepository;
import com.drawthon.team1.team.repository.TeamMembershipRepository;
import com.drawthon.team1.team.repository.TeamRepository;
import com.drawthon.team1.team.web.dto.TeamResponses;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TeamService {

    private static final int INVITATION_VALID_DAYS = 7;

    private final TeamRepository teamRepository;
    private final TeamMembershipRepository membershipRepository;
    private final TeamInvitationRepository invitationRepository;
    private final MemberRepository memberRepository;
    private final String frontendBaseUrl;

    public TeamService(
            TeamRepository teamRepository,
            TeamMembershipRepository membershipRepository,
            TeamInvitationRepository invitationRepository,
            MemberRepository memberRepository,
            @Value("${app.frontend-base-url:http://localhost:5173}") String frontendBaseUrl) {
        this.teamRepository = teamRepository;
        this.membershipRepository = membershipRepository;
        this.invitationRepository = invitationRepository;
        this.memberRepository = memberRepository;
        this.frontendBaseUrl = removeTrailingSlash(frontendBaseUrl);
    }

    public TeamResponses.TeamDetail createTeam(Long memberId, String name) {
        Member owner = getMember(memberId);
        String normalizedName = normalizeTeamName(name);

        Team team = teamRepository.save(Team.create(normalizedName, owner));
        TeamMembership ownerMembership = membershipRepository.save(
                TeamMembership.create(team, owner, TeamRole.OWNER));

        return TeamResponses.TeamDetail.from(team, List.of(ownerMembership));
    }

    @Transactional(readOnly = true)
    public List<TeamResponses.TeamSummary> getMyTeams(Long memberId) {
        getMember(memberId);
        return membershipRepository.findAllWithTeamByMemberId(memberId).stream()
                .map(membership -> TeamResponses.TeamSummary.from(
                        membership,
                        membershipRepository.countByTeamId(membership.getTeam().getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public TeamResponses.TeamDetail getTeam(Long memberId, Long teamId) {
        requireMembership(teamId, memberId);
        Team team = getTeam(teamId);
        List<TeamMembership> memberships = membershipRepository.findAllWithMemberByTeamId(teamId);
        return TeamResponses.TeamDetail.from(team, memberships);
    }

    public TeamResponses.Invitation createInvitation(Long memberId, Long teamId) {
        TeamMembership membership = requireMembership(teamId, memberId);
        if (membership.getRole() != TeamRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "팀장만 초대 링크를 만들 수 있습니다.");
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        TeamInvitation invitation = invitationRepository.save(TeamInvitation.create(
                token,
                membership.getTeam(),
                membership.getMember(),
                LocalDateTime.now().plusDays(INVITATION_VALID_DAYS)));

        return toInvitationResponse(invitation);
    }

    @Transactional(readOnly = true)
    public TeamResponses.InvitationPreview getInvitationPreview(Long memberId, String token) {
        getMember(memberId);
        TeamInvitation invitation = getValidInvitation(token);
        Team team = invitation.getTeam();
        return new TeamResponses.InvitationPreview(
                team.getId(),
                team.getName(),
                team.getOwner().getNickname(),
                membershipRepository.countByTeamId(team.getId()),
                invitation.getExpiresAt(),
                membershipRepository.existsByTeamIdAndMemberId(team.getId(), memberId));
    }

    public TeamResponses.JoinResult acceptInvitation(Long memberId, String token) {
        Member member = getMember(memberId);
        TeamInvitation invitation = getValidInvitation(token);
        Team team = invitation.getTeam();

        if (membershipRepository.existsByTeamIdAndMemberId(team.getId(), memberId)) {
            return new TeamResponses.JoinResult(team.getId(), team.getName(), false);
        }

        membershipRepository.save(TeamMembership.create(team, member, TeamRole.MEMBER));
        return new TeamResponses.JoinResult(team.getId(), team.getName(), true);
    }

    private TeamResponses.Invitation toInvitationResponse(TeamInvitation invitation) {
        String inviteUrl = createInviteUrl(invitation.getToken());
        return TeamResponses.Invitation.from(invitation, inviteUrl);
    }

    private String createInviteUrl(String token) {
        return UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .pathSegment("invitations", token)
                .build()
                .toUriString();
    }

    private TeamInvitation getValidInvitation(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "초대 링크가 존재하지 않습니다.");
        }
        TeamInvitation invitation = invitationRepository.findWithTeamByToken(token)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "초대 링크가 존재하지 않습니다."));
        if (invitation.isExpired(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "만료된 초대 링크입니다.");
        }
        return invitation;
    }

    private TeamMembership requireMembership(Long teamId, Long memberId) {
        return membershipRepository.findByTeamIdAndMemberId(teamId, memberId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "해당 팀에 접근할 권한이 없습니다."));
    }

    private Team getTeam(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "팀이 존재하지 않습니다."));
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));
    }

    private String normalizeTeamName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("팀 이름은 필수입니다.");
        }
        String normalized = name.trim();
        if (normalized.length() > 50) {
            throw new IllegalArgumentException("팀 이름은 50자 이하여야 합니다.");
        }
        return normalized;
    }

    private static String removeTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
