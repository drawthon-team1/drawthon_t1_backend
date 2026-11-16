package com.drawthon.team1.sessionlogin.config;

import com.drawthon.team1.entity.Member;
import com.drawthon.team1.entity.MemberRole;
import com.drawthon.team1.sessionlogin.repository.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@ConditionalOnProperty(name = "app.auth.dev-admin.enabled", havingValue = "true")
public class DevAdminInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DevAdminInitializer.class);

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String nickname;

    public DevAdminInitializer(MemberRepository memberRepository,
                               PasswordEncoder passwordEncoder,
                               @Value("${app.auth.dev-admin.email}") String email,
                               @Value("${app.auth.dev-admin.password}") String password,
                               @Value("${app.auth.dev-admin.nickname}") String nickname) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.nickname = nickname;
    }

    @Override
    public void run(ApplicationArguments args) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        memberRepository.findByEmail(normalizedEmail).ifPresentOrElse(member -> {
            if (member.getRole() != MemberRole.ADMIN) {
                log.warn("개발 관리자 이메일이 이미 일반 회원으로 사용 중입니다: {}", normalizedEmail);
            }
        }, () -> {
            Member admin = Member.create(
                    normalizedEmail,
                    passwordEncoder.encode(password),
                    nickname.trim(),
                    MemberRole.ADMIN);
            memberRepository.save(admin);
            log.info("개발용 ADMIN 계정을 생성했습니다: {}", normalizedEmail);
        });
    }
}
