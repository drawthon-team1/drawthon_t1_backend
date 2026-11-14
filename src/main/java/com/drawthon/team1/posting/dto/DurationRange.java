package com.drawthon.team1.posting.dto;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 공고 목록 필터: 봉사(활동) 기간. 기간 = 봉사 종료일 - 시작일 + 1일.
 * 기획의 선택지를 그대로 따른다. 8~13일짜리 공고는 어느 선택지에도 해당하지 않는다.
 */
public enum DurationRange {
    /** 하루 */
    ONE_DAY,
    /** 3일 이하 (하루 포함) */
    UP_TO_3_DAYS,
    /** 일주일 이하 (3일 이하 포함) */
    UP_TO_1_WEEK,
    /** 2주 이상 1개월 미만 */
    TWO_WEEKS_TO_1_MONTH,
    /** 1개월 이상 */
    ONE_MONTH_OR_MORE;

    public boolean matches(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) {
            return false;
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        boolean oneMonthOrMore = !end.plusDays(1).isBefore(start.plusMonths(1));
        return switch (this) {
            case ONE_DAY -> days == 1;
            case UP_TO_3_DAYS -> days <= 3;
            case UP_TO_1_WEEK -> days <= 7;
            case TWO_WEEKS_TO_1_MONTH -> days >= 14 && !oneMonthOrMore;
            case ONE_MONTH_OR_MORE -> oneMonthOrMore;
        };
    }
}
