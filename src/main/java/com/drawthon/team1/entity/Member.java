package com.drawthon.team1.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.ColumnDefault;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ColumnDefault("'USER'")
    private MemberRole role = MemberRole.USER;

    protected Member() { } // JPA용

    private Member(String email, String pwHash, String nickname, MemberRole role) {
        this.email = email;
        this.pwHash = pwHash;
        this.nickname = nickname;
        this.role = role;
    }

    public static Member create(String email, String pwHash, String nickname){
        return create(email, pwHash, nickname, MemberRole.USER);
    }

    public static Member create(String email, String pwHash, String nickname, MemberRole role) {
        return new Member(email, pwHash, nickname, role);
    }
}
