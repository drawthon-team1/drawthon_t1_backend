package com.drawthon.team1.sessionlogin.service;

import com.drawthon.team1.entity.Member;
import com.drawthon.team1.sessionlogin.repository.MemberRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;


@Service
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Member signup(String email, String rawPassword, String nickname) {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("이메일은 필수입니다.");
        if (rawPassword == null || rawPassword.isBlank()) throw new IllegalArgumentException("비밀번호는 필수입니다.");
        if (nickname == null || nickname.isBlank()) throw new IllegalArgumentException("닉네임은 필수입니다.");

        validatePasswordLength(rawPassword);
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        if (memberRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.");
        }

        String hash = passwordEncoder.encode(rawPassword);
        Member member = Member.create(normalizedEmail, hash, nickname.trim());

        return memberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public Member login(String email, String rawPassword) {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("이메일은 필수입니다.");
        if (rawPassword == null || rawPassword.isBlank()) throw new IllegalArgumentException("비밀번호는 필수입니다.");

        validatePasswordLength(rawPassword);
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        Member member = memberRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(rawPassword, member.getPwHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return member;
    }

    @Transactional(readOnly = true)
    public Member getMember(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원이 존재하지 않습니다."));
    }

    private void validatePasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("비밀번호는 UTF-8 기준 72바이트 이하여야 합니다.");
        }
    }
}
