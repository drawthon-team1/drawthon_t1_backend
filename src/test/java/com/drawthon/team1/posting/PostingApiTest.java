package com.drawthon.team1.posting;

import com.drawthon.team1.posting.client.VolunteerApiClient;
import com.drawthon.team1.posting.dto.PostingExternalDTO;
import com.drawthon.team1.posting.entity.Post;
import com.drawthon.team1.posting.repository.PostApplicationRepository;
import com.drawthon.team1.posting.repository.PostingRepository;
import com.drawthon.team1.posting.service.PostingService;
import com.drawthon.team1.user.entity.Role;
import com.drawthon.team1.user.entity.User;
import com.drawthon.team1.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 인증은 아직 merge되지 않았으므로 보안 필터를 끄고 공고 기능만 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class PostingApiTest {
    private static final String BASE = "/api/v1/posting";
    private static final String IMPORT_BODY = "{\"programRegistrationNumber\":\"3425935\"}";

    @Autowired private MockMvc mockMvc;
    @Autowired private PostingService postingService;
    @Autowired private PostingRepository postingRepository;
    @Autowired private PostApplicationRepository postApplicationRepository;
    @Autowired private UserRepository userRepository;
    @MockitoBean private VolunteerApiClient volunteerApiClient;

    private Long adminId;
    private Long userId;

    @BeforeEach
    void setUp() {
        adminId = saveUser(Role.ADMIN);
        userId = saveUser(Role.USER);
    }

    @AfterEach
    void tearDown() {
        postApplicationRepository.deleteAll();
        postingRepository.deleteAll();
        userRepository.deleteAll();
    }

    private Long saveUser(Role role) {
        return userRepository.save(User.builder()
                .name("테스터")
                .email(UUID.randomUUID() + "@test.com")
                .role(role)
                .build()).getId();
    }

    private Post.PostBuilder recruitingPost() {
        return Post.builder()
                .programRegistrationNumber("3425935")
                .programTitle("환경 정화 봉사")
                .recruitmentStartDate(LocalDate.now().minusDays(1))
                .recruitmentEndDate(LocalDate.now().plusDays(1))
                .recruitmentCapacity(5);
    }

    private Long savePost(Post post) {
        return postingRepository.save(post).getId();
    }

    private static String updateBody(String title) {
        return """
                {"programTitle":"%s","programContent":"내용","programStartDate":"2026-12-01","programEndDate":"2026-12-02",
                 "recruitmentStartDate":"2026-11-01","recruitmentEndDate":"2026-11-30","recruitmentCapacity":3,"activityPlace":"서울"}
                """.formatted(title);
    }

    // ---- 직접 생성 ----

    private static String createBody(String title, LocalDate recruitmentStart, LocalDate recruitmentEnd) {
        return """
                {"programTitle":"%s","programContent":"내용","programStartDate":"2030-12-01","programEndDate":"2030-12-02",
                 "recruitmentStartDate":"%s","recruitmentEndDate":"%s","recruitmentCapacity":3,"activityPlace":"서울"}
                """.formatted(title, recruitmentStart, recruitmentEnd);
    }

    private static String recruitingBody(String title) {
        return createBody(title, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
    }

    @Test
    void adminCreatesPostingDirectlyWithout1365AndItAppearsInList() throws Exception {
        mockMvc.perform(post(BASE + "/" + adminId).contentType(MediaType.APPLICATION_JSON).content(recruitingBody("직접 작성 공고")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.programTitle").value("직접 작성 공고"))
                .andExpect(jsonPath("$.programRegistrationNumber").isEmpty())
                .andExpect(jsonPath("$.applicantCount").value(0));

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].programTitle").value("직접 작성 공고"));

        verifyNoInteractions(volunteerApiClient);
    }

    @Test
    void directPostingsCanCoexistWithoutProgramRegistrationNumber() throws Exception {
        for (String title : List.of("공고1", "공고2")) {
            mockMvc.perform(post(BASE + "/" + adminId).contentType(MediaType.APPLICATION_JSON).content(recruitingBody(title)))
                    .andExpect(status().isOk());
        }

        assertThat(postingRepository.count()).isEqualTo(2);
    }

    @Test
    void userCanApplyToDirectPosting() throws Exception {
        Post post = postingRepository.save(Post.builder()
                .programTitle("직접 작성 공고")
                .recruitmentStartDate(LocalDate.now().minusDays(1))
                .recruitmentEndDate(LocalDate.now().plusDays(1))
                .recruitmentCapacity(1)
                .build());

        mockMvc.perform(post(BASE + "/" + userId + "/" + post.getId() + "/application"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicantCount").value(1));
    }

    @Test
    void userCannotCreateDirectly() throws Exception {
        mockMvc.perform(post(BASE + "/" + userId).contentType(MediaType.APPLICATION_JSON).content(recruitingBody("공고")))
                .andExpect(status().isForbidden());

        assertThat(postingRepository.count()).isZero();
    }

    @Test
    void directCreateRejectsInvalidInput() throws Exception {
        mockMvc.perform(post(BASE + "/" + adminId).contentType(MediaType.APPLICATION_JSON).content(recruitingBody(" ")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(BASE + "/" + adminId).contentType(MediaType.APPLICATION_JSON).content(IMPORT_BODY))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(BASE + "/" + adminId).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("공고", LocalDate.now().plusDays(1), LocalDate.now())))
                .andExpect(status().isBadRequest());

        assertThat(postingRepository.count()).isZero();
    }

    // ---- 1365 공고 등록 ----

    @Test
    void adminCreatesPostingFrom1365() throws Exception {
        given(volunteerApiClient.fetchDetail("3425935")).willReturn(Optional.of(recruitingPost().build()));

        mockMvc.perform(post(BASE + "/" + adminId + "/external").contentType(MediaType.APPLICATION_JSON).content(IMPORT_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.programTitle").value("환경 정화 봉사"))
                .andExpect(jsonPath("$.applicantCount").value(0));

        assertThat(postingRepository.existsByProgramRegistrationNumber("3425935")).isTrue();
    }

    @Test
    void userCannotCreateAndExternalApiIsNotCalled() throws Exception {
        mockMvc.perform(post(BASE + "/" + userId + "/external").contentType(MediaType.APPLICATION_JSON).content(IMPORT_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("권한이 없습니다."));

        verifyNoInteractions(volunteerApiClient);
    }

    @Test
    void createRejectsDuplicate() throws Exception {
        savePost(recruitingPost().build());

        mockMvc.perform(post(BASE + "/" + adminId + "/external").contentType(MediaType.APPLICATION_JSON).content(IMPORT_BODY))
                .andExpect(status().isConflict());

        verifyNoInteractions(volunteerApiClient);
    }

    @Test
    void createReturns404WhenMissingIn1365() throws Exception {
        given(volunteerApiClient.fetchDetail("3425935")).willReturn(Optional.empty());

        mockMvc.perform(post(BASE + "/" + adminId + "/external").contentType(MediaType.APPLICATION_JSON).content(IMPORT_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void createRejectsBlankProgramRegistrationNumber() throws Exception {
        mockMvc.perform(post(BASE + "/" + adminId + "/external").contentType(MediaType.APPLICATION_JSON).content("{\"programRegistrationNumber\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPropagatesUpstreamFailure() throws Exception {
        given(volunteerApiClient.fetchDetail("3425935"))
                .willThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "1365 API 호출에 실패했습니다."));

        mockMvc.perform(post(BASE + "/" + adminId + "/external").contentType(MediaType.APPLICATION_JSON).content(IMPORT_BODY))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("1365 API 호출에 실패했습니다."));
    }

    @Test
    void unknownUserIsNotFound() throws Exception {
        mockMvc.perform(post(BASE + "/999999/external").contentType(MediaType.APPLICATION_JSON).content(IMPORT_BODY))
                .andExpect(status().isNotFound());
    }

    // ---- 수정 ----

    @Test
    void adminUpdatesPosting() throws Exception {
        Long postingId = savePost(recruitingPost().build());

        mockMvc.perform(put(BASE + "/" + adminId + "/" + postingId).contentType(MediaType.APPLICATION_JSON).content(updateBody("수정된 제목")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.programTitle").value("수정된 제목"))
                .andExpect(jsonPath("$.programStartDate").value("2026-12-01"))
                .andExpect(jsonPath("$.recruitmentCapacity").value(3));

        assertThat(postingRepository.findById(postingId).orElseThrow().getProgramTitle()).isEqualTo("수정된 제목");
    }

    @Test
    void userCannotUpdate() throws Exception {
        Long postingId = savePost(recruitingPost().build());

        mockMvc.perform(put(BASE + "/" + userId + "/" + postingId).contentType(MediaType.APPLICATION_JSON).content(updateBody("x")))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateRejectsInvalidPeriod() throws Exception {
        Long postingId = savePost(recruitingPost().build());
        String body = updateBody("x").replace("\"recruitmentEndDate\":\"2026-11-30\"", "\"recruitmentEndDate\":\"2026-10-01\"");

        mockMvc.perform(put(BASE + "/" + adminId + "/" + postingId).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    // ---- 삭제 ----

    @Test
    void adminDeletesPostingWithApplications() throws Exception {
        Long postingId = savePost(recruitingPost().build());
        postingService.applyPosting(userId, postingId);

        mockMvc.perform(delete(BASE + "/" + adminId + "/" + postingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(postingId))
                .andExpect(jsonPath("$.applicantCount").value(1));

        assertThat(postingRepository.existsById(postingId)).isFalse();
        assertThat(postApplicationRepository.count()).isZero();
    }

    @Test
    void userCannotDelete() throws Exception {
        Long postingId = savePost(recruitingPost().build());

        mockMvc.perform(delete(BASE + "/" + userId + "/" + postingId))
                .andExpect(status().isForbidden());

        assertThat(postingRepository.existsById(postingId)).isTrue();
    }

    // ---- 조회 ----

    @Test
    void listAndDetailAreAvailableWithoutRoleCheck() throws Exception {
        savePost(recruitingPost().programRegistrationNumber("1").build());
        Long latestId = savePost(recruitingPost().programRegistrationNumber("2").programContent("상세 내용").managerPhoneNumber("02-000-0000").build());

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(latestId))
                .andExpect(jsonPath("$[0].programTitle").value("환경 정화 봉사"))
                .andExpect(jsonPath("$[0].programContent").doesNotExist())
                .andExpect(jsonPath("$[0].managerPhoneNumber").doesNotExist())
                .andExpect(jsonPath("$[0].applicantCount").doesNotExist());

        mockMvc.perform(get(BASE + "/" + latestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.programRegistrationNumber").value("2"))
                .andExpect(jsonPath("$.programContent").value("상세 내용"))
                .andExpect(jsonPath("$.managerPhoneNumber").value("02-000-0000"))
                .andExpect(jsonPath("$.applicantCount").value(0));
    }

    @Test
    void detailReturns404WhenMissing() throws Exception {
        mockMvc.perform(get(BASE + "/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("공고를 찾을 수 없습니다."));
    }

    @Test
    void externalSearchRoutesToClientAndValidatesPaging() throws Exception {
        given(volunteerApiClient.search(any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .willReturn(List.of(PostingExternalDTO.builder().programRegistrationNumber("7").build()));

        mockMvc.perform(get(BASE + "/external").param("keyword", "환경"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].programRegistrationNumber").value("7"));

        mockMvc.perform(get(BASE + "/external").param("numOfRows", "101"))
                .andExpect(status().isBadRequest());
    }

    // ---- 신청 / 철회 ----

    @Test
    void userAppliesAndWithdrawsAndCanReapply() throws Exception {
        Long postingId = savePost(recruitingPost().build());
        String url = BASE + "/" + userId + "/" + postingId + "/application";

        mockMvc.perform(post(url)).andExpect(status().isOk()).andExpect(jsonPath("$.applicantCount").value(1));
        mockMvc.perform(delete(url)).andExpect(status().isOk()).andExpect(jsonPath("$.applicantCount").value(0));
        mockMvc.perform(post(url)).andExpect(status().isOk()).andExpect(jsonPath("$.applicantCount").value(1));
    }

    @Test
    void adminCannotApplyOrWithdraw() throws Exception {
        Long postingId = savePost(recruitingPost().build());
        String url = BASE + "/" + adminId + "/" + postingId + "/application";

        mockMvc.perform(post(url)).andExpect(status().isForbidden());
        mockMvc.perform(delete(url)).andExpect(status().isForbidden());
    }

    @Test
    void duplicateApplyIsConflict() throws Exception {
        Long postingId = savePost(recruitingPost().build());
        String url = BASE + "/" + userId + "/" + postingId + "/application";

        mockMvc.perform(post(url)).andExpect(status().isOk());
        mockMvc.perform(post(url)).andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value("이미 신청한 공고입니다."));
    }

    @Test
    void applyOutsideRecruitingPeriodIsConflict() throws Exception {
        LocalDate today = LocalDate.now();
        Long ended = savePost(recruitingPost().programRegistrationNumber("ended").recruitmentEndDate(today.minusDays(1)).recruitmentStartDate(today.minusDays(5)).build());
        Long notStarted = savePost(recruitingPost().programRegistrationNumber("future").recruitmentStartDate(today.plusDays(1)).recruitmentEndDate(today.plusDays(5)).build());
        Long lastDay = savePost(recruitingPost().programRegistrationNumber("last").recruitmentEndDate(today).build());

        mockMvc.perform(post(BASE + "/" + userId + "/" + ended + "/application")).andExpect(status().isConflict());
        mockMvc.perform(post(BASE + "/" + userId + "/" + notStarted + "/application")).andExpect(status().isConflict());
        mockMvc.perform(post(BASE + "/" + userId + "/" + lastDay + "/application")).andExpect(status().isOk());
    }

    @Test
    void applyOverCapacityIsConflictButZeroCapacityMeansUnlimited() throws Exception {
        Long limited = savePost(recruitingPost().programRegistrationNumber("limited").recruitmentCapacity(1).build());
        Long unlimited = savePost(recruitingPost().programRegistrationNumber("unlimited").recruitmentCapacity(0).build());
        postingService.applyPosting(saveUser(Role.USER), limited);

        mockMvc.perform(post(BASE + "/" + userId + "/" + limited + "/application"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("모집 인원이 마감되었습니다."));
        mockMvc.perform(post(BASE + "/" + userId + "/" + unlimited + "/application")).andExpect(status().isOk());
    }

    @Test
    void withdrawWithoutApplicationIsNotFound() throws Exception {
        Long postingId = savePost(recruitingPost().build());

        mockMvc.perform(delete(BASE + "/" + userId + "/" + postingId + "/application"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("신청 내역이 없습니다."));
    }

    @Test
    void concurrentApplicationsDoNotExceedCapacity() throws Exception {
        int capacity = 2;
        int applicants = 8;
        Long postingId = savePost(recruitingPost().recruitmentCapacity(capacity).build());
        List<Long> userIds = new ArrayList<>();
        for (int i = 0; i < applicants; i++) {
            userIds.add(saveUser(Role.USER));
        }

        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(applicants)) {
            for (Long id : userIds) {
                results.add(executor.submit(() -> {
                    start.await();
                    try {
                        postingService.applyPosting(id, postingId);
                        return true;
                    } catch (ResponseStatusException e) {
                        return false;
                    }
                }));
            }
            start.countDown();
        }

        long succeeded = 0;
        for (Future<Boolean> result : results) {
            if (result.get()) {
                succeeded++;
            }
        }
        assertThat(succeeded).isEqualTo(capacity);
        assertThat(postApplicationRepository.countByPostId(postingId)).isEqualTo(capacity);
    }
}
