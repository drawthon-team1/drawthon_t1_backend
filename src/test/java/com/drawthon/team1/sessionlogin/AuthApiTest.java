package com.drawthon.team1.sessionlogin;

import com.drawthon.team1.sessionlogin.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 세션으로 가입 → 로그인 → 공고 API 접근까지 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private MemberRepository memberRepository;

    @AfterEach
    void tearDown() {
        memberRepository.deleteAll();
    }

    private ResultActions signup(String email, String password, String role) throws Exception {
        String roleField = role == null ? "" : ",\"role\":\"%s\"".formatted(role);
        String content = "{\"email\":\"%s\",\"password\":\"%s\",\"nickname\":\"테스터\"%s}".formatted(email, password, roleField);
        return mockMvc.perform(post("/api/auth/signup").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(content));
    }

    private MockHttpSession login(String email, String password) throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/api/auth/login").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk());
        return session;
    }

    @Test
    void signupWithoutRoleCreatesUser() throws Exception {
        signup("user@test.com", "password1234", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void signupWithAdminRoleCreatesAdmin() throws Exception {
        signup("admin@test.com", "password1234", "ADMIN")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void signupRejectsUnknownRole() throws Exception {
        signup("bad-role@test.com", "password1234", "OWNER").andExpect(status().isBadRequest());
    }

    @Test
    void userCanReadPostingsButCannotCreate() throws Exception {
        signup("user2@test.com", "password1234", "USER").andExpect(status().isCreated());
        MockHttpSession session = login("user2@test.com", "password1234");

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"));
        mockMvc.perform(get("/api/v1/posting").session(session)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/posting").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(validPosting()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminSignedUpWithRoleCanCreatePosting() throws Exception {
        signup("admin2@test.com", "password1234", "ADMIN").andExpect(status().isCreated());
        MockHttpSession session = login("admin2@test.com", "password1234");

        mockMvc.perform(post("/api/v1/posting").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(validPosting()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.programTitle").value("환경 정화 봉사"));
    }

    private static String validPosting() {
        return """
                {"programTitle":"환경 정화 봉사","programStartDate":"2030-12-01","programEndDate":"2030-12-02",
                 "recruitmentStartDate":"2030-11-01","recruitmentEndDate":"2030-11-30","recruitmentCapacity":3,"reward":50000}
                """;
    }
}
