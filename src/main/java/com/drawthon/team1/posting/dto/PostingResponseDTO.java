package com.drawthon.team1.posting.dto;

import com.drawthon.team1.posting.entity.Post;
import com.drawthon.team1.posting.entity.RewardType;
import com.drawthon.team1.posting.entity.WorkType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 공고 생성 / 수정 / 삭제 / 신청 / 철회 공통 반환 dto
 */

@Getter
@Builder
public class PostingResponseDTO {
    private final Long id;
    private final String programRegistrationNumber;
    private final String programTitle;
    private final String programContent;
    private final LocalDate programStartDate;
    private final LocalDate programEndDate;
    private final String activityStartTime;
    private final String activityEndTime;
    private final LocalDate recruitmentStartDate;
    private final LocalDate recruitmentEndDate;
    private final Integer recruitmentCapacity;
    /** 이 서비스에서 신청한 인원 */
    private final long applicantCount;
    private final String registeringOrganizationName;
    private final String activityPlace;
    /** VOLUNTEER_HOURS(1365 공고, reward = 봉사시간) 또는 WAGE(직접 작성 공고, reward = 임금 원) */
    private final RewardType rewardType;
    private final Integer reward;
    private final String address;
    private final WorkType workType;

    public static PostingResponseDTO from(Post post, long applicantCount) {
        return PostingResponseDTO.builder()
                .id(post.getId())
                .programRegistrationNumber(post.getProgramRegistrationNumber())
                .programTitle(post.getProgramTitle())
                .programContent(post.getProgramContent())
                .programStartDate(post.getProgramStartDate())
                .programEndDate(post.getProgramEndDate())
                .activityStartTime(post.getActivityStartTime())
                .activityEndTime(post.getActivityEndTime())
                .recruitmentStartDate(post.getRecruitmentStartDate())
                .recruitmentEndDate(post.getRecruitmentEndDate())
                .recruitmentCapacity(post.getRecruitmentCapacity())
                .applicantCount(applicantCount)
                .registeringOrganizationName(post.getRegisteringOrganizationName())
                .activityPlace(post.getActivityPlace())
                .rewardType(post.getRewardType())
                .reward(post.getReward())
                .address(post.getAddress())
                .workType(post.getWorkType())
                .build();
    }
}
