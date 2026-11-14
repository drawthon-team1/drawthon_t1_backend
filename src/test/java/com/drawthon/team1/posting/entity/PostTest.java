package com.drawthon.team1.posting.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PostTest {

    @Test
    void volunteerHoursIsEndMinusStart() {
        assertThat(Post.volunteerHours("9", "18")).isEqualTo(9);
        assertThat(Post.volunteerHours("13", "18")).isEqualTo(5);
        assertThat(Post.volunteerHours("09:00", "13:30")).isEqualTo(4);
    }

    @Test
    void volunteerHoursIsNullWhenItCannotBeCalculated() {
        assertThat(Post.volunteerHours(null, "18")).isNull();
        assertThat(Post.volunteerHours("9", " ")).isNull();
        assertThat(Post.volunteerHours("18", "9")).isNull();
        assertThat(Post.volunteerHours("9", "9")).isNull();
        assertThat(Post.volunteerHours("오전", "오후")).isNull();
        assertThat(Post.volunteerHours("9:00", "9:40")).isNull();
    }

    @Test
    void recruitmentStatusIsCalculatedFromPeriodAndCapacity() {
        LocalDate today = LocalDate.of(2030, 3, 10);
        Post.PostBuilder open = Post.builder().recruitmentStartDate(today.minusDays(5)).recruitmentCapacity(2);

        assertThat(open.recruitmentEndDate(today.plusDays(4)).build().recruitmentStatus(today, 0)).isEqualTo(RecruitmentStatus.RECRUITING);
        assertThat(open.recruitmentEndDate(today.plusDays(3)).build().recruitmentStatus(today, 0)).isEqualTo(RecruitmentStatus.CLOSING_SOON);
        assertThat(open.recruitmentEndDate(today).build().recruitmentStatus(today, 1)).isEqualTo(RecruitmentStatus.CLOSING_SOON);
        assertThat(open.recruitmentEndDate(today.minusDays(1)).build().recruitmentStatus(today, 0)).isEqualTo(RecruitmentStatus.CLOSED);
        assertThat(open.recruitmentEndDate(today.plusDays(30)).build().recruitmentStatus(today, 2)).isEqualTo(RecruitmentStatus.CLOSED);
        assertThat(Post.builder().recruitmentStartDate(today.plusDays(1)).build().recruitmentStatus(today, 0))
                .isEqualTo(RecruitmentStatus.UPCOMING);
        // 모집 기간과 인원이 없는 공고는 계속 모집 중, 모집 인원 0은 제한 없음
        assertThat(Post.builder().build().recruitmentStatus(today, 100)).isEqualTo(RecruitmentStatus.RECRUITING);
        assertThat(Post.builder().recruitmentCapacity(0).build().recruitmentStatus(today, 100)).isEqualTo(RecruitmentStatus.RECRUITING);
    }

    @Test
    void isLocatedInComparesWholeWordsOfAddress() {
        Post post = Post.builder().address("인천광역시 남동구 덕골로 96 (수산동, 청송오리)").build();

        assertThat(post.isLocatedIn("남동구")).isTrue();
        assertThat(post.isLocatedIn("수산동")).isTrue();
        assertThat(post.isLocatedIn("인천광역시 남동구")).isTrue();
        assertThat(post.isLocatedIn("동구")).isFalse();
        assertThat(post.isLocatedIn("포천시")).isFalse();
        assertThat(Post.builder().build().isLocatedIn("남동구")).isFalse();
    }

    @Test
    void normalizeAddressDrops1365Placeholder() {
        assertThat(Post.normalizeAddress(" 경기도 포천시 군내면 꽃배산길 86 ")).isEqualTo("경기도 포천시 군내면 꽃배산길 86");
        assertThat(Post.normalizeAddress("수요처 지도")).isNull();
        assertThat(Post.normalizeAddress(" ")).isNull();
        assertThat(Post.normalizeAddress(null)).isNull();
    }

    @Test
    void workTypeIsInferredFromKeywords() {
        assertThat(WorkType.infer("치자따기 농촌일손돕기", null)).isEqualTo(WorkType.HARVEST);
        assertThat(WorkType.infer("농산물 선별 및 포장 봉사", "")).isEqualTo(WorkType.SORTING_PACKING);
        assertThat(WorkType.infer("봄맞이 봉사", "고추 모종 심기 활동")).isEqualTo(WorkType.PLANTING_CULTIVATION);
        assertThat(WorkType.infer("텃밭 잡초 제거", null)).isEqualTo(WorkType.WEEDING);
        assertThat(WorkType.infer("농촌마을 작은도서관 봉사활동", null)).isEqualTo(WorkType.ETC);
        assertThat(WorkType.infer(null, null)).isEqualTo(WorkType.ETC);
    }

    @Test
    void rewardTypeFollowsWherePostingCameFrom() {
        assertThat(Post.builder().programRegistrationNumber("3425935").build().getRewardType())
                .isEqualTo(RewardType.VOLUNTEER_HOURS);
        assertThat(Post.builder().build().getRewardType()).isEqualTo(RewardType.WAGE);
    }
}
