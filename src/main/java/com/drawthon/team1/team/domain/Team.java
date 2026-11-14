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
import lombok.Getter;

@Entity
@Table(name = "teams")
@Getter
public class Team extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private Member owner;

    protected Team() {
    }

    private Team(String name, Member owner) {
        this.name = name;
        this.owner = owner;
    }

    public static Team create(String name, Member owner) {
        return new Team(name, owner);
    }
}
