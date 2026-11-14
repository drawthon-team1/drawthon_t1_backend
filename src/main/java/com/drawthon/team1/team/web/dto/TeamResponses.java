package com.drawthon.team1.team.web.dto;

import com.drawthon.team1.team.domain.Team;
import com.drawthon.team1.team.domain.TeamInvitation;
import com.drawthon.team1.team.domain.TeamMembership;
import com.drawthon.team1.team.domain.TeamRole;

import java.time.LocalDateTime;
import java.util.List;

public final class TeamResponses {

    private TeamResponses() {
    }

    public record TeamSummary(
            Long id,
            String name,
            Long ownerId,
            String ownerNickname,
            TeamRole myRole,
            long memberCount,
            LocalDateTime createdAt) {

        public static TeamSummary from(TeamMembership membership, long memberCount) {
            Team team = membership.getTeam();
            return new TeamSummary(
                    team.getId(),
                    team.getName(),
                    team.getOwner().getId(),
                    team.getOwner().getNickname(),
                    membership.getRole(),
                    memberCount,
                    team.getCreatedAt());
        }
    }

    public record TeamMember(
            Long memberId,
            String nickname,
            TeamRole role,
            LocalDateTime joinedAt) {

        public static TeamMember from(TeamMembership membership) {
            return new TeamMember(
                    membership.getMember().getId(),
                    membership.getMember().getNickname(),
                    membership.getRole(),
                    membership.getCreatedAt());
        }
    }

    public record TeamDetail(
            Long id,
            String name,
            Long ownerId,
            String ownerNickname,
            int memberCount,
            LocalDateTime createdAt,
            List<TeamMember> members) {

        public static TeamDetail from(Team team, List<TeamMembership> memberships) {
            return new TeamDetail(
                    team.getId(),
                    team.getName(),
                    team.getOwner().getId(),
                    team.getOwner().getNickname(),
                    memberships.size(),
                    team.getCreatedAt(),
                    memberships.stream().map(TeamMember::from).toList());
        }
    }

    public record Invitation(
            String token,
            Long teamId,
            String teamName,
            String inviteUrl,
            LocalDateTime expiresAt) {

        public static Invitation from(TeamInvitation invitation, String inviteUrl) {
            return new Invitation(
                    invitation.getToken(),
                    invitation.getTeam().getId(),
                    invitation.getTeam().getName(),
                    inviteUrl,
                    invitation.getExpiresAt());
        }
    }

    public record InvitationPreview(
            Long teamId,
            String teamName,
            String ownerNickname,
            long memberCount,
            LocalDateTime expiresAt,
            boolean alreadyJoined) {
    }

    public record JoinResult(
            Long teamId,
            String teamName,
            boolean joined) {
    }
}
