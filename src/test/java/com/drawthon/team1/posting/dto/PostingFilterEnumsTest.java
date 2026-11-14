package com.drawthon.team1.posting.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PostingFilterEnumsTest {
    private static final LocalDate START = LocalDate.of(2030, 3, 1);

    private static boolean duration(DurationRange range, int days) {
        return range.matches(START, START.plusDays(days - 1));
    }

    @Test
    void durationRangesFollowThePlannedOptions() {
        assertThat(duration(DurationRange.ONE_DAY, 1)).isTrue();
        assertThat(duration(DurationRange.ONE_DAY, 2)).isFalse();

        assertThat(duration(DurationRange.UP_TO_3_DAYS, 1)).isTrue();
        assertThat(duration(DurationRange.UP_TO_3_DAYS, 3)).isTrue();
        assertThat(duration(DurationRange.UP_TO_3_DAYS, 4)).isFalse();

        assertThat(duration(DurationRange.UP_TO_1_WEEK, 7)).isTrue();
        assertThat(duration(DurationRange.UP_TO_1_WEEK, 8)).isFalse();

        assertThat(duration(DurationRange.TWO_WEEKS_TO_1_MONTH, 13)).isFalse();
        assertThat(duration(DurationRange.TWO_WEEKS_TO_1_MONTH, 14)).isTrue();
        assertThat(duration(DurationRange.TWO_WEEKS_TO_1_MONTH, 30)).isTrue();
        assertThat(duration(DurationRange.TWO_WEEKS_TO_1_MONTH, 31)).isFalse();

        // 3월 1일 ~ 3월 31일은 한 달
        assertThat(duration(DurationRange.ONE_MONTH_OR_MORE, 30)).isFalse();
        assertThat(duration(DurationRange.ONE_MONTH_OR_MORE, 31)).isTrue();
        assertThat(duration(DurationRange.ONE_MONTH_OR_MORE, 90)).isTrue();
    }

    @Test
    void eightToThirteenDaysMatchNoOption() {
        for (int days = 8; days <= 13; days++) {
            for (DurationRange range : DurationRange.values()) {
                assertThat(duration(range, days)).as("%s, %d일", range, days).isFalse();
            }
        }
    }

    @Test
    void durationNeedsBothDates() {
        assertThat(DurationRange.ONE_DAY.matches(null, START)).isFalse();
        assertThat(DurationRange.ONE_DAY.matches(START, null)).isFalse();
        assertThat(DurationRange.ONE_DAY.matches(START, START.minusDays(1))).isFalse();
    }

    @Test
    void timeSlotMatchesWhenActivityOverlaps() {
        // 9시 ~ 17시
        assertThat(TimeSlot.DAWN.matches(540, 1020)).isFalse();
        assertThat(TimeSlot.MORNING.matches(540, 1020)).isTrue();
        assertThat(TimeSlot.AFTERNOON.matches(540, 1020)).isTrue();
        assertThat(TimeSlot.EVENING.matches(540, 1020)).isFalse();
        // 12시에 끝나면 오후와는 겹치지 않는다
        assertThat(TimeSlot.AFTERNOON.matches(540, 720)).isFalse();
        // 19시 ~ 23시
        assertThat(TimeSlot.EVENING.matches(1140, 1380)).isTrue();
        assertThat(TimeSlot.NIGHT.matches(1140, 1380)).isTrue();
    }

    @Test
    void timeSlotUsesStartOnlyWhenEndIsMissingOrEarlier() {
        assertThat(TimeSlot.MORNING.matches(540, null)).isTrue();
        assertThat(TimeSlot.AFTERNOON.matches(540, null)).isFalse();
        // 22시 시작, 다음 날 6시 종료
        assertThat(TimeSlot.NIGHT.matches(1320, 360)).isTrue();
        assertThat(TimeSlot.DAWN.matches(1320, 360)).isFalse();
        assertThat(TimeSlot.MORNING.matches(null, 720)).isFalse();
    }
}
