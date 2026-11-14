package com.drawthon.team1.team.controller;

import com.drawthon.team1.team.service.TeamService;
import com.drawthon.team1.team.web.dto.CreateTeamRequest;
import com.drawthon.team1.team.web.dto.TeamResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Team API", description = "팀 생성, 초대 및 조회 API")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class TeamApiController {

    private final TeamService teamService;

    public TeamApiController(TeamService teamService) {
        this.teamService = teamService;
    }

    @Operation(summary = "팀 만들기")
    @PostMapping("/teams")
    @ResponseStatus(HttpStatus.CREATED)
    public TeamResponses.TeamDetail createTeam(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody CreateTeamRequest request) {
        return teamService.createTeam(memberId, request.getName());
    }

    @Operation(summary = "내 팀 목록 조회")
    @GetMapping("/teams")
    public List<TeamResponses.TeamSummary> getMyTeams(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {
        return teamService.getMyTeams(memberId);
    }

    @Operation(summary = "팀 현황 조회")
    @GetMapping("/teams/{teamId}")
    public TeamResponses.TeamDetail getTeam(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long teamId) {
        return teamService.getTeam(memberId, teamId);
    }

    @Operation(summary = "팀 초대 링크 생성", description = "팀장만 생성할 수 있으며 링크는 7일간 유효합니다.")
    @PostMapping("/teams/{teamId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public TeamResponses.Invitation createInvitation(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long teamId) {
        return teamService.createInvitation(memberId, teamId);
    }

    @Operation(summary = "초대 정보 조회")
    @GetMapping("/team-invitations/{token}")
    public TeamResponses.InvitationPreview getInvitation(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable String token) {
        return teamService.getInvitationPreview(memberId, token);
    }

    @Operation(summary = "팀 초대 수락")
    @PostMapping("/team-invitations/{token}/accept")
    public TeamResponses.JoinResult acceptInvitation(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable String token) {
        return teamService.acceptInvitation(memberId, token);
    }
}
