package com.drawthon.team1.posting;

import com.drawthon.team1.entity.Member;
import com.drawthon.team1.entity.MemberRole;
import com.drawthon.team1.posting.client.VolunteerApiClient;
import com.drawthon.team1.posting.dto.PostingExternalDTO;
import com.drawthon.team1.posting.entity.Post;
import com.drawthon.team1.posting.repository.PostApplicationRepository;
import com.drawthon.team1.posting.repository.PostingRepository;
import com.drawthon.team1.posting.service.PostingService;
import com.drawthon.team1.sessionlogin.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 보안 필터를 켠 상태로 검증한다. 로그인은 세션 대신 인증 객체(principal = 회원 ID)를 직접 넣는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PostingApiTest {
    private static final String BASE = "/api/v1/posting";
    private static final String IMPORT_BODY = "{\"programRegistrationNumber\":\"3425935\"}";

    @Autowired private MockMvc mockMvc;
    @Autowired private PostingService postingService;
    @Autowired private PostingRepository postingRepository;
    @Autowired private PostApplicationRepository postApplicationRepository;
    @Autowired private MemberRepository memberRepository;
    @MockitoBean private VolunteerApiClient volunteerApiClient;

    private Long adminId;
    private Long userId;

    @BeforeEach
    void setUp() {
        adminId = saveMember(MemberRole.ADMIN);
        userId = saveMember(MemberRole.USER);
    }

    @AfterEach
    void tearDown() {
        postApplicationRepository.deleteAll();
        postingRepository.deleteAll();
        memberRepository.deleteAll();
    }

    private Long saveMember(MemberRole role) {
        return memberRepository.save(Member.create(UUID.randomUUID() + "@test.com", "hash", "테스터", role)).getId();
    }

    private static RequestPostProcessor loginAs(Long memberId) {
        return authentication(UsernamePasswordAuthenticationToken.authenticated(memberId, null, List.of()));
    }

    /** memberId로 로그인한 회원이 CSRF 토큰을 담아 보낸 요청 */
    private ResultActions as(Long memberId, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.with(loginAs(memberId)).with(csrf()));
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
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

    private static String createBody(String title, LocalDate recruitmentStart, LocalDate recruitmentEnd) {
        return """
                {"programTitle":"%s","programContent":"내용","programStartDate":"2030-12-01","programEndDate":"2030-12-02",
                 "recruitmentStartDate":"%s","recruitmentEndDate":"%s","recruitmentCapacity":3,"activityPlace":"서울","rewardType":"WAGE","reward":50000}
                """.formatted(title, recruitmentStart, recruitmentEnd);
    }

    private static String recruitingBody(String title) {
        return createBody(title, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
    }

    private static String updateBody(String title) {
        return """
                {"programTitle":"%s","programContent":"내용","programStartDate":"2026-12-01","programEndDate":"2026-12-02",
                 "activityStartTime":"9","activityEndTime":"13",
                 "recruitmentStartDate":"2026-11-01","recruitmentEndDate":"2026-11-30","recruitmentCapacity":3,"activityPlace":"서울",
                 "rewardType":"WAGE","reward":70000}
                """.formatted(title);
    }

    // ---- 보안 ----

    @Test
    void anonymousRequestsAreUnauthorized() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        mockMvc.perform(json(post(BASE), recruitingBody("공고")).with(csrf())).andExpect(status().isUnauthorized());

        assertThat(postingRepository.count()).isZero();
    }

    @Test
    void changeRequestWithoutCsrfIsForbidden() throws Exception {
        mockMvc.perform(json(post(BASE), recruitingBody("공고")).with(loginAs(adminId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("CSRF 토큰이 누락되었거나 유효하지 않습니다."));

        assertThat(postingRepository.count()).isZero();
    }

    @Test
    void deletedMemberSessionIsRejected() throws Exception {
        Long postingId = savePost(recruitingPost().build());
        memberRepository.deleteById(userId);

        as(userId, post(BASE + "/" + postingId + "/application"))
                .andExpect(status().isUnauthorized());
    }

    // ---- 직접 생성 ----

    @Test
    void adminCreatesPostingDirectlyWithout1365AndItAppearsInList() throws Exception {
        as(adminId, json(post(BASE), recruitingBody("직접 작성 공고")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.programTitle").value("직접 작성 공고"))
                .andExpect(jsonPath("$.programRegistrationNumber").isEmpty())
                .andExpect(jsonPath("$.applicantCount").value(0))
                .andExpect(jsonPath("$.rewardType").value("WAGE"))
                .andExpect(jsonPath("$.reward").value(50000));

        as(userId, get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].programTitle").value("직접 작성 공고"))
                .andExpect(jsonPath("$[0].rewardType").value("WAGE"))
                .andExpect(jsonPath("$[0].reward").value(50000));

        verifyNoInteractions(volunteerApiClient);
    }

    @Test
    void directPostingsCanCoexistWithoutProgramRegistrationNumber() throws Exception {
        for (String title : List.of("공고1", "공고2")) {
            as(adminId, json(post(BASE), recruitingBody(title))).andExpect(status().isOk());
        }

        assertThat(postingRepository.count()).isEqualTo(2);
    }

    @Test
    void userCanApplyToDirectPosting() throws Exception {
        Long postingId = savePost(Post.builder()
                .programTitle("직접 작성 공고")
                .recruitmentStartDate(LocalDate.now().minusDays(1))
                .recruitmentEndDate(LocalDate.now().plusDays(1))
                .recruitmentCapacity(1)
                .build());

        as(userId, post(BASE + "/" + postingId + "/application"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicantCount").value(1));
    }

    @Test
    void userCannotCreateDirectly() throws Exception {
        as(userId, json(post(BASE), recruitingBody("공고")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("권한이 없습니다."));

        assertThat(postingRepository.count()).isZero();
    }

    @Test
    void directCreateRequiresPositiveWage() throws Exception {
        as(adminId, json(post(BASE), recruitingBody("공고").replace(",\"reward\":50000", "")))
                .andExpect(status().isBadRequest());
        as(adminId, json(post(BASE), recruitingBody("공고").replace("\"reward\":50000", "\"reward\":0")))
                .andExpect(status().isBadRequest());

        assertThat(postingRepository.count()).isZero();
    }

    @Test
    void directCreateRejectsInvalidInput() throws Exception {
        as(adminId, json(post(BASE), recruitingBody(" "))).andExpect(status().isBadRequest());
        as(adminId, json(post(BASE), IMPORT_BODY)).andExpect(status().isBadRequest());
        as(adminId, json(post(BASE), createBody("공고", LocalDate.now().plusDays(1), LocalDate.now())))
                .andExpect(status().isBadRequest());

        assertThat(postingRepository.count()).isZero();
    }

    // ---- 1365 공고 등록 ----

    @Test
    void adminImportsPostingFrom1365() throws Exception {
        given(volunteerApiClient.fetchDetail("3425935"))
                .willReturn(Optional.of(recruitingPost().activityStartTime("8").activityEndTime("16").reward(8).build()));

        as(adminId, json(post(BASE + "/external"), IMPORT_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.programTitle").value("환경 정화 봉사"))
                .andExpect(jsonPath("$.applicantCount").value(0))
                .andExpect(jsonPath("$.rewardType").value("VOLUNTEER_HOURS"))
                .andExpect(jsonPath("$.reward").value(8));

        assertThat(postingRepository.existsByProgramRegistrationNumber("3425935")).isTrue();
    }

    @Test
    void userCannotImportAndExternalApiIsNotCalled() throws Exception {
        as(userId, json(post(BASE + "/external"), IMPORT_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("권한이 없습니다."));

        verifyNoInteractions(volunteerApiClient);
    }

    @Test
    void importRejectsDuplicate() throws Exception {
        savePost(recruitingPost().build());

        as(adminId, json(post(BASE + "/external"), IMPORT_BODY)).andExpect(status().isConflict());

        verifyNoInteractions(volunteerApiClient);
    }

    @Test
    void importReturns404WhenMissingIn1365() throws Exception {
        given(volunteerApiClient.fetchDetail("3425935")).willReturn(Optional.empty());

        as(adminId, json(post(BASE + "/external"), IMPORT_BODY)).andExpect(status().isNotFound());
    }

    @Test
    void importRejectsBlankProgramRegistrationNumber() throws Exception {
        as(adminId, json(post(BASE + "/external"), "{\"programRegistrationNumber\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importPropagatesUpstreamFailure() throws Exception {
        given(volunteerApiClient.fetchDetail("3425935"))
                .willThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "1365 API 호출에 실패했습니다."));

        as(adminId, json(post(BASE + "/external"), IMPORT_BODY))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("1365 API 호출에 실패했습니다."));
    }

    // ---- 수정 ----

    @Test
    void adminUpdatesPosting() throws Exception {
        Long postingId = savePost(recruitingPost().build());

        as(adminId, json(put(BASE + "/" + postingId), updateBody("수정된 제목")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.programTitle").value("수정된 제목"))
                .andExpect(jsonPath("$.programStartDate").value("2026-12-01"))
                .andExpect(jsonPath("$.recruitmentCapacity").value(3));

        assertThat(postingRepository.findById(postingId).orElseThrow().getProgramTitle()).isEqualTo("수정된 제목");
    }

    @Test
    void update1365PostingRecalculatesVolunteerHoursAndIgnoresWage() throws Exception {
        Long postingId = savePost(recruitingPost().activityStartTime("8").activityEndTime("16").reward(8).build());

        as(adminId, json(put(BASE + "/" + postingId), updateBody("시간 변경")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rewardType").value("VOLUNTEER_HOURS"))
                .andExpect(jsonPath("$.reward").value(4));
    }

    @Test
    void updateDirectPostingChangesWage() throws Exception {
        Long postingId = savePost(Post.builder().programTitle("직접 작성 공고").reward(50000).build());

        as(adminId, json(put(BASE + "/" + postingId), updateBody("임금 변경")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rewardType").value("WAGE"))
                .andExpect(jsonPath("$.reward").value(70000));
    }

    @Test
    void userCannotUpdate() throws Exception {
        Long postingId = savePost(recruitingPost().build());

        as(userId, json(put(BASE + "/" + postingId), updateBody("x"))).andExpect(status().isForbidden());
    }

    @Test
    void updateRejectsInvalidPeriod() throws Exception {
        Long postingId = savePost(recruitingPost().build());
        String body = updateBody("x").replace("\"recruitmentEndDate\":\"2026-11-30\"", "\"recruitmentEndDate\":\"2026-10-01\"");

        as(adminId, json(put(BASE + "/" + postingId), body)).andExpect(status().isBadRequest());
    }

    // ---- 삭제 ----

    @Test
    void adminDeletesPostingWithApplications() throws Exception {
        Long postingId = savePost(recruitingPost().build());
        postingService.applyPosting(userId, postingId);

        as(adminId, delete(BASE + "/" + postingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(postingId))
                .andExpect(jsonPath("$.applicantCount").value(1));

        assertThat(postingRepository.existsById(postingId)).isFalse();
        assertThat(postApplicationRepository.count()).isZero();
    }

    @Test
    void userCannotDelete() throws Exception {
        Long postingId = savePost(recruitingPost().build());

        as(userId, delete(BASE + "/" + postingId)).andExpect(status().isForbidden());

        assertThat(postingRepository.existsById(postingId)).isTrue();
    }

    // ---- 조회 ----

    @Test
    void listShowsSummaryAndDetailShowsAll() throws Exception {
        savePost(recruitingPost().programRegistrationNumber("1").build());
        Long latestId = savePost(recruitingPost().programRegistrationNumber("2").programContent("상세 내용").build());

        as(userId, get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(latestId))
                .andExpect(jsonPath("$[0].programTitle").value("환경 정화 봉사"))
                .andExpect(jsonPath("$[0].rewardType").value("VOLUNTEER_HOURS"))
                .andExpect(jsonPath("$[0].programContent").doesNotExist())
                .andExpect(jsonPath("$[0].applicantCount").value(0));

        as(adminId, get(BASE + "/" + latestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.programRegistrationNumber").value("2"))
                .andExpect(jsonPath("$.programContent").value("상세 내용"))
                .andExpect(jsonPath("$.applicantCount").value(0));
    }

    @Test
    void detailReturns404WhenMissing() throws Exception {
        as(userId, get(BASE + "/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("공고를 찾을 수 없습니다."));
    }

    @Test
    void externalSearchRoutesToClientAndValidatesPaging() throws Exception {
        given(volunteerApiClient.search(any(), anyInt(), anyInt()))
                .willReturn(List.of(PostingExternalDTO.builder().programRegistrationNumber("7").build()));

        as(adminId, get(BASE + "/external").param("keyword", "환경"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].programRegistrationNumber").value("7"));

        as(adminId, get(BASE + "/external").param("numOfRows", "101"))
                .andExpect(status().isBadRequest());
    }

    // ---- 신청 / 철회 ----

    @Test
    void userAppliesAndWithdrawsAndCanReapply() throws Exception {
        Long postingId = savePost(recruitingPost().build());
        String url = BASE + "/" + postingId + "/application";

        as(userId, post(url)).andExpect(status().isOk()).andExpect(jsonPath("$.applicantCount").value(1));
        as(userId, delete(url)).andExpect(status().isOk()).andExpect(jsonPath("$.applicantCount").value(0));
        as(userId, post(url)).andExpect(status().isOk()).andExpect(jsonPath("$.applicantCount").value(1));
    }

    @Test
    void adminCannotApplyOrWithdraw() throws Exception {
        Long postingId = savePost(recruitingPost().build());
        String url = BASE + "/" + postingId + "/application";

        as(adminId, post(url)).andExpect(status().isForbidden());
        as(adminId, delete(url)).andExpect(status().isForbidden());
    }

    @Test
    void duplicateApplyIsConflict() throws Exception {
        Long postingId = savePost(recruitingPost().build());
        String url = BASE + "/" + postingId + "/application";

        as(userId, post(url)).andExpect(status().isOk());
        as(userId, post(url)).andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value("이미 신청한 공고입니다."));
    }

    @Test
    void applyOutsideRecruitingPeriodIsConflict() throws Exception {
        LocalDate today = LocalDate.now();
        Long ended = savePost(recruitingPost().programRegistrationNumber("ended").recruitmentEndDate(today.minusDays(1)).recruitmentStartDate(today.minusDays(5)).build());
        Long notStarted = savePost(recruitingPost().programRegistrationNumber("future").recruitmentStartDate(today.plusDays(1)).recruitmentEndDate(today.plusDays(5)).build());
        Long lastDay = savePost(recruitingPost().programRegistrationNumber("last").recruitmentEndDate(today).build());

        as(userId, post(BASE + "/" + ended + "/application")).andExpect(status().isConflict());
        as(userId, post(BASE + "/" + notStarted + "/application")).andExpect(status().isConflict());
        as(userId, post(BASE + "/" + lastDay + "/application")).andExpect(status().isOk());
    }

    @Test
    void applyOverCapacityIsConflictButZeroCapacityMeansUnlimited() throws Exception {
        Long limited = savePost(recruitingPost().programRegistrationNumber("limited").recruitmentCapacity(1).build());
        Long unlimited = savePost(recruitingPost().programRegistrationNumber("unlimited").recruitmentCapacity(0).build());
        postingService.applyPosting(saveMember(MemberRole.USER), limited);

        as(userId, post(BASE + "/" + limited + "/application"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("모집 인원이 마감되었습니다."));
        as(userId, post(BASE + "/" + unlimited + "/application")).andExpect(status().isOk());
    }

    @Test
    void withdrawWithoutApplicationIsNotFound() throws Exception {
        Long postingId = savePost(recruitingPost().build());

        as(userId, delete(BASE + "/" + postingId + "/application"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("신청 내역이 없습니다."));
    }

    @Test
    void concurrentApplicationsDoNotExceedCapacity() throws Exception {
        int capacity = 2;
        int applicants = 8;
        Long postingId = savePost(recruitingPost().recruitmentCapacity(capacity).build());
        List<Long> memberIds = new ArrayList<>();
        for (int i = 0; i < applicants; i++) {
            memberIds.add(saveMember(MemberRole.USER));
        }

        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(applicants)) {
            for (Long id : memberIds) {
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
