package com.drawthon.team1.posting;

import com.drawthon.team1.entity.Member;
import com.drawthon.team1.entity.MemberRole;
import com.drawthon.team1.posting.client.VolunteerApiClient;
import com.drawthon.team1.posting.repository.PostingRepository;
import com.drawthon.team1.sessionlogin.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 로그인 API로 만든 세션으로 공고 API의 권한이 동작하는지 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PostingSessionLoginTest {
    private static final String PASSWORD = "password1234";
    private static final String POSTING = """
            {"programTitle":"고구마 수확 일손","programStartDate":"2030-12-01","programEndDate":"2030-12-02",
             "recruitmentStartDate":"2030-11-01","recruitmentEndDate":"2030-11-30","recruitmentCapacity":3,"rewardType":"WAGE","reward":50000}
            """;

    @Autowired private MockMvc mockMvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private PostingRepository postingRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @MockitoBean private VolunteerApiClient volunteerApiClient;

    @AfterEach
    void tearDown() {
        postingRepository.deleteAll();
        memberRepository.deleteAll();
    }

    private MockHttpSession loginAs(String email, MemberRole role) throws Exception {
        memberRepository.save(Member.create(email, passwordEncoder.encode(PASSWORD), "테스터", role));
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/api/auth/login").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)))
                .andExpect(status().isOk());
        return session;
    }

    @Test
    void loggedInUserCanReadPostingsButCannotCreate() throws Exception {
        MockHttpSession session = loginAs("user@test.com", MemberRole.USER);

        mockMvc.perform(get("/api/v1/posting").session(session)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/posting").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(POSTING))
                .andExpect(status().isForbidden());
    }

    @Test
    void loggedInAdminCanCreatePosting() throws Exception {
        MockHttpSession session = loginAs("admin@test.com", MemberRole.ADMIN);

        mockMvc.perform(post("/api/v1/posting").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(POSTING))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rewardType").value("WAGE"))
                .andExpect(jsonPath("$.reward").value(50000));
    }
}
