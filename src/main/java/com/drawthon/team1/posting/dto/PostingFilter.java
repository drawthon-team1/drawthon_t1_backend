package com.drawthon.team1.posting.dto;

import com.drawthon.team1.posting.entity.Post;
import com.drawthon.team1.posting.entity.RecruitmentStatus;
import com.drawthon.team1.posting.entity.RewardType;
import com.drawthon.team1.posting.entity.WorkType;

import java.time.LocalDate;
import java.util.List;

/**
 * 공고 목록 필터. 값이 없는 조건은 적용하지 않는다.
 * 서로 다른 조건은 모두 만족해야 하고(AND), 한 조건에 여러 값을 고르면 하나만 맞아도 된다(OR).
 *
 * @param rewardType 공고 유형. VOLUNTEER_HOURS(봉사) / WAGE(일자리)
 * @param sigungu 시·군(구). 주소에 이 이름이 있어야 한다
 * @param eupMyeonDong 읍·면·동. 주소에 이 이름이 있어야 한다
 * @param startDate 일정 시작일. 이날 이후에도 봉사 기간이 이어지는 공고
 * @param endDate 일정 마감일. 이날 이전에 봉사 기간이 시작되는 공고
 */
public record PostingFilter(RewardType rewardType,
                            String sigungu,
                            String eupMyeonDong,
                            LocalDate startDate,
                            LocalDate endDate,
                            List<DurationRange> durations,
                            List<TimeSlot> timeSlots,
                            List<RecruitmentStatus> statuses,
                            List<WorkType> workTypes) {

    public static PostingFilter none() {
        return new PostingFilter(null, null, null, null, null, null, null, null, null);
    }

    public boolean matches(Post post, RecruitmentStatus status) {
        return matchesRewardType(post)
                && matchesRegion(post)
                && matchesSchedule(post)
                && matchesDuration(post)
                && matchesTimeSlot(post)
                && matchesStatus(status)
                && matchesWorkType(post);
    }

    private boolean matchesRewardType(Post post) {
        return rewardType == null || post.getRewardType() == rewardType;
    }

    private boolean matchesRegion(Post post) {
        return (isBlank(sigungu) || post.isLocatedIn(sigungu))
                && (isBlank(eupMyeonDong) || post.isLocatedIn(eupMyeonDong));
    }

    /** 봉사 기간이 선택한 일정과 하루라도 겹치는 공고. 봉사 기간이 없는 공고는 일정 조건이 있으면 제외한다. */
    private boolean matchesSchedule(Post post) {
        if (startDate == null && endDate == null) {
            return true;
        }
        LocalDate programStart = post.getProgramStartDate();
        LocalDate programEnd = post.getProgramEndDate();
        if (programStart == null || programEnd == null) {
            return false;
        }
        return (startDate == null || !programEnd.isBefore(startDate))
                && (endDate == null || !programStart.isAfter(endDate));
    }

    private boolean matchesDuration(Post post) {
        return isEmpty(durations)
                || durations.stream().anyMatch(range -> range.matches(post.getProgramStartDate(), post.getProgramEndDate()));
    }

    private boolean matchesTimeSlot(Post post) {
        return isEmpty(timeSlots)
                || timeSlots.stream().anyMatch(slot -> slot.matches(post.activityStartMinutes(), post.activityEndMinutes()));
    }

    /** RECRUITING(모집 중)을 고르면 마감 직전 공고도 포함한다. 마감 직전도 아직 모집 중이기 때문이다. */
    private boolean matchesStatus(RecruitmentStatus status) {
        if (isEmpty(statuses)) {
            return true;
        }
        return statuses.contains(status)
                || (status == RecruitmentStatus.CLOSING_SOON && statuses.contains(RecruitmentStatus.RECRUITING));
    }

    private boolean matchesWorkType(Post post) {
        return isEmpty(workTypes) || workTypes.contains(post.getWorkType());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean isEmpty(List<?> values) {
        return values == null || values.isEmpty();
    }
}
