package com.drawthon.team1.user;

import com.drawthon.team1.posting.client.VolunteerApiClient;
import com.drawthon.team1.posting.entity.Post;
import com.drawthon.team1.posting.entity.PostApplication;
import com.drawthon.team1.posting.repository.PostApplicationRepository;
import com.drawthon.team1.posting.repository.PostingRepository;
import com.drawthon.team1.user.entity.Role;
import com.drawthon.team1.user.entity.User;
import com.drawthon.team1.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 인증은 아직 merge되지 않았으므로 보안 필터를 끄고 사용자 기능만 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class UserApiTest {
    private static final String BASE = "/api/v1/user";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PostingRepository postingRepository;
    @Autowired private PostApplicationRepository postApplicationRepository;
    @MockitoBean private VolunteerApiClient volunteerApiClient;

    @AfterEach
    void tearDown() {
        postApplicationRepository.deleteAll();
        postingRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User saveUser(String email) {
        return userRepository.save(User.builder().name("홍길동").email(email).role(Role.USER).build());
    }

    private static String body(String name, String email) {
        return "{\"name\":\"%s\",\"email\":\"%s\"}".formatted(name, email);
    }

    @Test
    void 사용자를_생성하면_USER_권한으로_저장된다() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body("홍길동", "hong@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.email").value("hong@test.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void 요청에_role을_보내도_ADMIN으로_생성되지_않는다() throws Exception {
        String content = "{\"name\":\"홍길동\",\"email\":\"hong@test.com\",\"role\":\"ADMIN\"}";

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(content))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void 잘못된_입력으로_생성하면_400을_반환한다() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body("", "not-an-email")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 중복된_이메일로_생성하면_409를_반환한다() throws Exception {
        saveUser("hong@test.com");

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body("김철수", "hong@test.com")))
                .andExpect(status().isConflict());
    }

    @Test
    void 사용자를_단건과_목록으로_조회한다() throws Exception {
        User user = saveUser("hong@test.com");
        saveUser("kim@test.com");

        mockMvc.perform(get(BASE + "/" + user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("hong@test.com"));
        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void 없는_사용자를_조회하면_404를_반환한다() throws Exception {
        mockMvc.perform(get(BASE + "/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void 사용자_정보를_수정한다() throws Exception {
        User user = saveUser("hong@test.com");

        mockMvc.perform(put(BASE + "/" + user.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body("홍수정", "new@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("홍수정"))
                .andExpect(jsonPath("$.email").value("new@test.com"))
                .andExpect(jsonPath("$.role").value("USER"));

        assertThat(userRepository.findById(user.getId()).orElseThrow().getEmail()).isEqualTo("new@test.com");
    }

    @Test
    void 자신의_이메일을_유지한_채_수정할_수_있다() throws Exception {
        User user = saveUser("hong@test.com");

        mockMvc.perform(put(BASE + "/" + user.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body("홍수정", "hong@test.com")))
                .andExpect(status().isOk());
    }

    @Test
    void 다른_사용자의_이메일로_수정하면_409를_반환한다() throws Exception {
        User user = saveUser("hong@test.com");
        saveUser("kim@test.com");

        mockMvc.perform(put(BASE + "/" + user.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body("홍길동", "kim@test.com")))
                .andExpect(status().isConflict());
    }

    @Test
    void 공고를_신청한_사용자를_삭제하면_신청_내역도_함께_삭제된다() throws Exception {
        User user = saveUser("hong@test.com");
        Post post = postingRepository.save(Post.builder()
                .programRegistrationNumber("3425935")
                .programTitle("환경 정화 봉사")
                .build());
        postApplicationRepository.save(PostApplication.builder().post(post).user(user).build());

        mockMvc.perform(delete(BASE + "/" + user.getId()))
                .andExpect(status().isOk());

        assertThat(userRepository.existsById(user.getId())).isFalse();
        assertThat(postApplicationRepository.countByPostId(post.getId())).isZero();
    }

    @Test
    void 없는_사용자를_삭제하면_404를_반환한다() throws Exception {
        mockMvc.perform(delete(BASE + "/999999"))
                .andExpect(status().isNotFound());
    }
}
