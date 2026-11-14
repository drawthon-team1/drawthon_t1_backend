package com.drawthon.team1.sessionlogin;

import com.drawthon.team1.entity.Member;
import com.drawthon.team1.entity.MemberRole;
import com.drawthon.team1.sessionlogin.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SessionAuthApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void regularSignupAlwaysCreatesUser() throws Exception {
        String email = uniqueEmail("user");

        mockMvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson(email, "일반회원")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void onlyAdminCanCreateAdmin() throws Exception {
        Member user = createMember("user", MemberRole.USER);
        Member admin = createMember("admin", MemberRole.ADMIN);

        mockMvc.perform(post("/api/auth/admin/signup")
                        .with(authenticatedAs(user))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson(uniqueEmail("denied"), "권한없음")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/auth/admin/signup")
                        .with(authenticatedAs(admin))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson(uniqueEmail("new-admin"), "신규관리자")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    private Member createMember(String prefix, MemberRole role) {
        return memberRepository.save(Member.create(uniqueEmail(prefix), "test-password-hash", prefix, role));
    }

    private RequestPostProcessor authenticatedAs(Member member) {
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                member.getId(),
                null,
                List.of(new SimpleGrantedAuthority(member.getRole().authority())));
        return authentication(authentication);
    }

    private String uniqueEmail(String prefix) {
        return prefix + "+" + UUID.randomUUID() + "@example.com";
    }

    private String signupJson(String email, String nickname) {
        return """
                {"email":"%s","password":"password1234","nickname":"%s"}
                """.formatted(email, nickname);
    }
}
