package com.drawthon.team1.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "member", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_member_email",
                columnNames = "email")
})
@Getter
public class Member extends BaseTimeEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String email;

    @Column(name = "pw_hash", nullable = false, length = 200)
    private String pwHash;

    @Column(nullable = false, length = 40)
    private String nickname;

    protected Member() { } // JPA용

    private Member(String email, String pwHash, String nickname) {
        this.email = email;
        this.pwHash = pwHash;
        this.nickname = nickname;
    }

    public static Member create(String email, String pwHash, String nickname){
        return new Member(email, pwHash, nickname);
    }
}
