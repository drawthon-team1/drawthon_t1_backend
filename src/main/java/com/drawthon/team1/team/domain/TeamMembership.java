package com.drawthon.team1.team.domain;

import com.drawthon.team1.entity.BaseTimeEntity;
import com.drawthon.team1.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

@Entity
@Table(name = "team_membership", uniqueConstraints = {
        @UniqueConstraint(name = "uk_team_membership_team_member", columnNames = {"team_id", "member_id"})
})
@Getter
public class TeamMembership extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TeamRole role;

    protected TeamMembership() {
    }

    private TeamMembership(Team team, Member member, TeamRole role) {
        this.team = team;
        this.member = member;
        this.role = role;
    }

    public static TeamMembership create(Team team, Member member, TeamRole role) {
        return new TeamMembership(team, member, role);
    }
}
