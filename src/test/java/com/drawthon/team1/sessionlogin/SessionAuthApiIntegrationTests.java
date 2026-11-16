package com.drawthon.team1.sessionlogin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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
    void publicAdminSignupCreatesAdminInDevelopment() throws Exception {
        mockMvc.perform(post("/api/auth/admin/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson(uniqueEmail("new-admin"), "신규관리자")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void defaultDevAdminCanLogin() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@drawthon.local","password":"drawthon-admin-1234"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
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
