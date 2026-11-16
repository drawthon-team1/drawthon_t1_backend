package com.drawthon.team1.posting;

import com.drawthon.team1.entity.Member;
import com.drawthon.team1.entity.MemberRole;
import com.drawthon.team1.posting.client.VolunteerApiClient;
import com.drawthon.team1.posting.entity.Post;
import com.drawthon.team1.posting.entity.PostApplication;
import com.drawthon.team1.posting.entity.WorkType;
import com.drawthon.team1.posting.repository.PostApplicationRepository;
import com.drawthon.team1.posting.repository.PostingRepository;
import com.drawthon.team1.sessionlogin.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 공고 목록 필터. 공고 6건을 넣고 조건별로 어떤 공고가 나오는지 제목으로 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PostingFilterApiTest {
    private static final String BASE = "/api/v1/posting";
    /** 1365 공고, 3개월, 9~17시, 포천시 군내면, 수확, 모집 중 */
    private static final String HOURS = "봉사 3개월";
    /** 직접 작성, 하루, 5~8시, 인천 남동구 수산동, 제초, 마감 직전 */
    private static final String ONE_DAY = "일자리 하루";
    /** 직접 작성, 20일, 19~23시, 대전 동구, 기타, 모집 기간 종료 */
    private static final String TWENTY_DAYS = "일자리 20일";
    /** 직접 작성, 3일, 시간·주소 없음, 선별·포장, 모집 인원 마감 */
    private static final String FULL = "일자리 3일 인원마감";
    /** 직접 작성, 10일, 파종·재배, 모집 시작 전 */
    private static final String UPCOMING = "일자리 10일 모집예정";

    @Autowired private MockMvc mockMvc;
    @Autowired private PostingRepository postingRepository;
    @Autowired private PostApplicationRepository postApplicationRepository;
    @Autowired private MemberRepository memberRepository;
    @MockitoBean private VolunteerApiClient volunteerApiClient;

    private Long memberId;

    @BeforeEach
    void setUp() {
        LocalDate today = LocalDate.now();
        Member member = memberRepository.save(Member.create(UUID.randomUUID() + "@test.com", "hash", "테스터", MemberRole.USER));
        memberId = member.getId();

        postingRepository.save(Post.builder().programRegistrationNumber("A1").programTitle(HOURS)
                .programStartDate(LocalDate.of(2030, 3, 1)).programEndDate(LocalDate.of(2030, 5, 31))
                .activityStartTime("9").activityEndTime("17").reward(8)
                .address("경기도 포천시 군내면 꽃배산길 86").workType(WorkType.HARVEST)
                .recruitmentStartDate(today.minusDays(1)).recruitmentEndDate(today.plusDays(10)).recruitmentCapacity(5).build());
        postingRepository.save(Post.builder().programTitle(ONE_DAY)
                .programStartDate(LocalDate.of(2030, 3, 10)).programEndDate(LocalDate.of(2030, 3, 10))
                .activityStartTime("05:00").activityEndTime("08:00").reward(50000)
                .address("인천광역시 남동구 덕골로 96 (수산동, 청송오리)").workType(WorkType.WEEDING)
                .recruitmentStartDate(today.minusDays(1)).recruitmentEndDate(today.plusDays(2)).recruitmentCapacity(5).build());
        postingRepository.save(Post.builder().programTitle(TWENTY_DAYS)
                .programStartDate(LocalDate.of(2030, 6, 1)).programEndDate(LocalDate.of(2030, 6, 20))
                .activityStartTime("19").activityEndTime("23").reward(90000)
                .address("대전광역시 동구 판교1길 3").workType(WorkType.ETC)
                .recruitmentStartDate(today.minusDays(10)).recruitmentEndDate(today.minusDays(1)).recruitmentCapacity(5).build());
        Post full = postingRepository.save(Post.builder().programTitle(FULL)
                .programStartDate(LocalDate.of(2030, 7, 1)).programEndDate(LocalDate.of(2030, 7, 3))
                .reward(70000).workType(WorkType.SORTING_PACKING)
                .recruitmentStartDate(today.minusDays(1)).recruitmentEndDate(today.plusDays(30)).recruitmentCapacity(1).build());
        postApplicationRepository.save(PostApplication.builder().post(full).member(member).build());
        postingRepository.save(Post.builder().programTitle(UPCOMING)
                .programStartDate(LocalDate.of(2030, 8, 1)).programEndDate(LocalDate.of(2030, 8, 10))
                .activityStartTime("13").activityEndTime("18").reward(60000)
                .address("전북특별자치도 군산시 성산면 깐치멀1길 27").workType(WorkType.PLANTING_CULTIVATION)
                .recruitmentStartDate(today.plusDays(5)).recruitmentEndDate(today.plusDays(40)).recruitmentCapacity(5).build());
    }

    @AfterEach
    void tearDown() {
        postApplicationRepository.deleteAll();
        postingRepository.deleteAll();
        memberRepository.deleteAll();
    }

    private ResultActions list(String... params) throws Exception {
        MockHttpServletRequestBuilder request = get(BASE)
                .with(authentication(UsernamePasswordAuthenticationToken.authenticated(memberId, null, List.of())));
        for (int i = 0; i < params.length; i += 2) {
            request.param(params[i], params[i + 1]);
        }
        return mockMvc.perform(request);
    }

    private void expectTitles(ResultActions result, String... titles) throws Exception {
        result.andExpect(status().isOk());
        if (titles.length == 0) {
            result.andExpect(jsonPath("$", empty()));
        } else {
            result.andExpect(jsonPath("$[*].programTitle", containsInAnyOrder(titles)));
        }
    }

    @Test
    void withoutFilterReturnsAllWithStatusAddressAndWorkType() throws Exception {
        expectTitles(list(), HOURS, ONE_DAY, TWENTY_DAYS, FULL, UPCOMING);

        list("statuses", "CLOSING_SOON")
                .andExpect(jsonPath("$[0].recruitmentStatus").value("CLOSING_SOON"))
                .andExpect(jsonPath("$[0].address").value("인천광역시 남동구 덕골로 96 (수산동, 청송오리)"))
                .andExpect(jsonPath("$[0].workType").value("WEEDING"))
                .andExpect(jsonPath("$[0].rewardType").value("WAGE"));
    }

    @Test
    void filtersByRewardType() throws Exception {
        expectTitles(list("rewardType", "VOLUNTEER_HOURS"), HOURS);
        expectTitles(list("rewardType", "WAGE"), ONE_DAY, TWENTY_DAYS, FULL, UPCOMING);
    }

    @Test
    void filtersByRegion() throws Exception {
        expectTitles(list("sigungu", "포천시"), HOURS);
        expectTitles(list("sigungu", "포천시", "eupMyeonDong", "군내면"), HOURS);
        expectTitles(list("eupMyeonDong", "수산동"), ONE_DAY);
        // "동구"는 대전 동구만 해당하고 인천 남동구는 해당하지 않는다
        expectTitles(list("sigungu", "동구"), TWENTY_DAYS);
        expectTitles(list("sigungu", "남동구", "eupMyeonDong", "군내면"));
    }

    @Test
    void filtersBySchedule() throws Exception {
        expectTitles(list("startDate", "2030-03-05", "endDate", "2030-03-15"), HOURS, ONE_DAY);
        expectTitles(list("startDate", "2030-07-02"), FULL, UPCOMING);
        expectTitles(list("endDate", "2030-03-01"), HOURS);
        expectTitles(list("startDate", "2031-01-01"));

        list("startDate", "2030-03-15", "endDate", "2030-03-05").andExpect(status().isBadRequest());
    }

    @Test
    void filtersByDuration() throws Exception {
        expectTitles(list("durations", "ONE_DAY"), ONE_DAY);
        expectTitles(list("durations", "UP_TO_3_DAYS"), ONE_DAY, FULL);
        expectTitles(list("durations", "TWO_WEEKS_TO_1_MONTH,ONE_MONTH_OR_MORE"), HOURS, TWENTY_DAYS);
        // 10일짜리 공고는 어느 기간 선택지에도 해당하지 않는다
        expectTitles(list("durations", "ONE_DAY,UP_TO_3_DAYS,UP_TO_1_WEEK,TWO_WEEKS_TO_1_MONTH,ONE_MONTH_OR_MORE"),
                HOURS, ONE_DAY, TWENTY_DAYS, FULL);
    }

    @Test
    void filtersByTimeSlot() throws Exception {
        expectTitles(list("timeSlots", "DAWN"), ONE_DAY);
        expectTitles(list("timeSlots", "MORNING"), HOURS, ONE_DAY);
        expectTitles(list("timeSlots", "AFTERNOON"), HOURS, UPCOMING);
        expectTitles(list("timeSlots", "EVENING", "timeSlots", "NIGHT"), TWENTY_DAYS);
    }

    @Test
    void filtersByRecruitmentStatus() throws Exception {
        // 모집 중을 고르면 마감 직전 공고도 함께 나온다
        expectTitles(list("statuses", "RECRUITING"), HOURS, ONE_DAY);
        expectTitles(list("statuses", "CLOSING_SOON"), ONE_DAY);
        expectTitles(list("statuses", "CLOSED"), TWENTY_DAYS, FULL);
        expectTitles(list("statuses", "UPCOMING"), UPCOMING);
    }

    @Test
    void filtersByWorkType() throws Exception {
        expectTitles(list("workTypes", "HARVEST,WEEDING"), HOURS, ONE_DAY);
        expectTitles(list("workTypes", "ETC"), TWENTY_DAYS);
    }

    @Test
    void combinesDifferentFiltersWithAnd() throws Exception {
        expectTitles(list("rewardType", "WAGE", "statuses", "RECRUITING"), ONE_DAY);
        expectTitles(list("rewardType", "VOLUNTEER_HOURS", "timeSlots", "MORNING", "workTypes", "HARVEST", "sigungu", "포천시"), HOURS);
        expectTitles(list("rewardType", "VOLUNTEER_HOURS", "statuses", "CLOSED"));
    }

    @Test
    void filtersByKeywordInTitleOrganizationPlaceOrAddress() throws Exception {
        expectTitles(list("keyword", "3개월"), HOURS);
        expectTitles(list("keyword", "포천"), HOURS);
        expectTitles(list("keyword", "일자리"), ONE_DAY, TWENTY_DAYS, FULL, UPCOMING);
        expectTitles(list("keyword", "  모집예정 "), UPCOMING);
        expectTitles(list("keyword", "없는 공고"));
    }

    @Test
    void listIncludesApplicantCount() throws Exception {
        list("keyword", "인원마감").andExpect(jsonPath("$[0].applicantCount").value(1));
        list("keyword", "3개월").andExpect(jsonPath("$[0].applicantCount").value(0));
        list("keyword", "3개월")
                .andExpect(jsonPath("$[0].activityStartTime").value("9"))
                .andExpect(jsonPath("$[0].activityEndTime").value("17"));
    }

    @Test
    void rejectsUnknownFilterValue() throws Exception {
        list("rewardType", "MONEY").andExpect(status().isBadRequest());
        list("durations", "FOREVER").andExpect(status().isBadRequest());
        list("startDate", "2030/03/01").andExpect(status().isBadRequest());
    }
}
