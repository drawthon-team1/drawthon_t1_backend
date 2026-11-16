package com.drawthon.team1.sessionlogin.controller;

import com.drawthon.team1.entity.Member;
import com.drawthon.team1.sessionlogin.service.MemberService;
import com.drawthon.team1.sessionlogin.web.dto.CsrfResponse;
import com.drawthon.team1.sessionlogin.web.dto.LoginRequest;
import com.drawthon.team1.sessionlogin.web.dto.MemberResponse;
import com.drawthon.team1.sessionlogin.web.dto.SignupRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Session Auth API", description = "세션 쿠키 기반 회원 인증 API")
public class SessionAuthApiController {
    private final MemberService memberService;
    private final SecurityContextRepository repository;
    private final boolean publicAdminSignupEnabled;

    public SessionAuthApiController(MemberService memberService,
                                    SecurityContextRepository repository,
                                    @Value("${app.auth.public-admin-signup-enabled:false}")
                                    boolean publicAdminSignupEnabled) {
        this.memberService = memberService;
        this.repository = repository;
        this.publicAdminSignupEnabled = publicAdminSignupEnabled;
    }

    @Operation(summary = "CSRF 토큰 발급", description = "Swagger UI에서 POST 요청 전에 먼저 실행1!!")
    @GetMapping("/csrf")
    public CsrfResponse csrf(@Parameter(hidden = true) CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getToken());
    }

    @Operation(summary = "회원가입")
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse signup(@Valid @RequestBody SignupRequest request) {
        return MemberResponse.from(memberService.signup(
                request.getEmail(), request.getPassword(), request.getNickname()));
    }

    @Operation(summary = "관리자 회원가입",
            description = "개발 설정에서 공개 관리자 가입이 활성화된 경우 ADMIN 계정을 생성합니다. "
                    + "운영에서는 PUBLIC_ADMIN_SIGNUP_ENABLED=false로 비활성화해야 합니다.")
    @PostMapping("/admin/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse signupAdmin(@Valid @RequestBody SignupRequest request) {
        if (!publicAdminSignupEnabled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "관리자 공개 회원가입이 비활성화되어 있습니다.");
        }
        return MemberResponse.from(memberService.signupAdmin(
                request.getEmail(), request.getPassword(), request.getNickname()));
    }

    @Operation(summary = "로그인", description = "성공 시 브라우저에 세션 쿠키 저장")
    @PostMapping("/login")
    public MemberResponse login(@Valid @RequestBody LoginRequest body,
            HttpServletRequest request, HttpServletResponse response) {
        Member member = memberService.login(body.getEmail(), body.getPassword());
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        var authorities = List.of(new SimpleGrantedAuthority(member.getRole().authority()));
        var authentication = UsernamePasswordAuthenticationToken.authenticated(member.getId(), null, authorities);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        repository.saveContext(context, request, response);
        return MemberResponse.from(member);
    }

    @Operation(summary = "내 정보 조회")
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public MemberResponse me(@Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {
        return MemberResponse.from(memberService.getMember(memberId));
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        var handler = new SecurityContextLogoutHandler();
        handler.setSecurityContextRepository(repository);
        handler.logout(request, response, SecurityContextHolder.getContext().getAuthentication());
    }
}
