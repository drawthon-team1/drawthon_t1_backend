package com.drawthon.team1.posting.dto;

import com.drawthon.team1.posting.entity.Post;
import com.drawthon.team1.posting.entity.RecruitmentStatus;
import com.drawthon.team1.posting.entity.RewardType;
import com.drawthon.team1.posting.entity.WorkType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 공고 조회 dto
 * 목록 조회는 요약 항목(활동 시간·신청 인원 포함)만, 단일 공고 조회는 내용까지 담는다. 값이 없는 항목은 응답에서 제외된다.
 */

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostingInfoDTO {
    private final Long id;
    private final String programRegistrationNumber;
    private final String programTitle;
    private final String registeringOrganizationName;
    private final String activityPlace;
    private final LocalDate programStartDate;
    private final LocalDate programEndDate;
    private final LocalDate recruitmentStartDate;
    private final LocalDate recruitmentEndDate;
    private final Integer recruitmentCapacity;
    /** VOLUNTEER_HOURS(1365 공고, reward = 봉사시간) 또는 WAGE(직접 작성 공고, reward = 임금 원) */
    private final RewardType rewardType;
    private final Integer reward;
    private final String address;
    private final WorkType workType;
    /** 조회 시점에 계산한 모집 상태: UPCOMING / RECRUITING / CLOSING_SOON / CLOSED */
    private final RecruitmentStatus recruitmentStatus;

    private final String programContent;
    private final String activityStartTime;
    private final String activityEndTime;
    /** 이 서비스에서 신청한 인원 */
    private final Long applicantCount;

    /** 목록 조회용 요약. 카드에 "신청 2/4명"을 보여 줄 수 있도록 신청 인원을 포함한다. */
    public static PostingInfoDTO from(Post post, long applicantCount, RecruitmentStatus recruitmentStatus) {
        return summaryBuilder(post, recruitmentStatus).applicantCount(applicantCount).build();
    }

    /** 단일 공고 조회용 전체 항목 */
    public static PostingInfoDTO detailFrom(Post post, long applicantCount, RecruitmentStatus recruitmentStatus) {
        return summaryBuilder(post, recruitmentStatus)
                .programContent(post.getProgramContent())
                .applicantCount(applicantCount)
                .build();
    }

    private static PostingInfoDTOBuilder summaryBuilder(Post post, RecruitmentStatus recruitmentStatus) {
        return PostingInfoDTO.builder()
                .id(post.getId())
                .programRegistrationNumber(post.getProgramRegistrationNumber())
                .programTitle(post.getProgramTitle())
                .registeringOrganizationName(post.getRegisteringOrganizationName())
                .activityPlace(post.getActivityPlace())
                .programStartDate(post.getProgramStartDate())
                .programEndDate(post.getProgramEndDate())
                .activityStartTime(post.getActivityStartTime())
                .activityEndTime(post.getActivityEndTime())
                .recruitmentStartDate(post.getRecruitmentStartDate())
                .recruitmentEndDate(post.getRecruitmentEndDate())
                .recruitmentCapacity(post.getRecruitmentCapacity())
                .rewardType(post.getRewardType())
                .reward(post.getReward())
                .address(post.getAddress())
                .workType(post.getWorkType())
                .recruitmentStatus(recruitmentStatus);
    }
}
