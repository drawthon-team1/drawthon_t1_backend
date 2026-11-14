package com.drawthon.team1.team.domain;

import com.drawthon.team1.entity.BaseTimeEntity;
import com.drawthon.team1.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Table(name = "team_invitation", uniqueConstraints = {
        @UniqueConstraint(name = "uk_team_invitation_token", columnNames = "token")
})
@Getter
public class TeamInvitation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private Member createdBy;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    protected TeamInvitation() {
    }

    private TeamInvitation(String token, Team team, Member createdBy, LocalDateTime expiresAt) {
        this.token = token;
        this.team = team;
        this.createdBy = createdBy;
        this.expiresAt = expiresAt;
    }

    public static TeamInvitation create(String token, Team team, Member createdBy, LocalDateTime expiresAt) {
        return new TeamInvitation(token, team, createdBy, expiresAt);
    }

    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }
}
