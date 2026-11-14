package com.drawthon.team1.posting.dto;

import com.drawthon.team1.posting.entity.Post;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 대시보드 읽어오기처럼 공고 글들을 가져오는 dto
 * 목록 조회는 요약 항목만, 단일 공고 조회는 전체 항목을 담는다. 값이 없는 항목은 응답에서 제외된다.
 *
 * 1365 api request & response DTO 파악하기
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
    private final String recruitmentStatus;
    private final LocalDate programStartDate;
    private final LocalDate programEndDate;
    private final LocalDate recruitmentStartDate;
    private final LocalDate recruitmentEndDate;
    private final Integer recruitmentCapacity;

    private final String programContent;
    private final String activityStartTime;
    private final String activityEndTime;
    private final Integer externalApplicantCount;
    /** 이 서비스에서 신청한 인원 */
    private final Long applicantCount;
    private final String activityWeekdays;
    private final String serviceCategoryCode;
    private final String adultParticipationAllowed;
    private final String youthParticipationAllowed;
    private final String groupParticipationAllowed;
    private final String publicOfficialParticipationAllowed;
    private final String familyParticipationAllowed;
    private final String hostOrganizationName;
    private final String managerName;
    private final String managerPhoneNumber;
    private final String managerFaxNumber;
    private final String managerAddress;
    private final String managerEmail;
    private final String provinceCode;
    private final String districtCode;
    private final String activityAddress1;
    private final String activityAddress2;
    private final String activityAddress3;
    private final String activityCoordinates1;
    private final String activityCoordinates2;
    private final String activityCoordinates3;

    /** 목록 조회용 요약 */
    public static PostingInfoDTO from(Post post) {
        return summaryBuilder(post).build();
    }

    /** 단일 공고 조회용 전체 항목 */
    public static PostingInfoDTO detailFrom(Post post, long applicantCount) {
        return summaryBuilder(post)
                .programContent(post.getProgramContent())
                .activityStartTime(post.getActivityStartTime())
                .activityEndTime(post.getActivityEndTime())
                .externalApplicantCount(post.getExternalApplicantCount())
                .applicantCount(applicantCount)
                .activityWeekdays(post.getActivityWeekdays())
                .serviceCategoryCode(post.getServiceCategoryCode())
                .adultParticipationAllowed(post.getAdultParticipationAllowed())
                .youthParticipationAllowed(post.getYouthParticipationAllowed())
                .groupParticipationAllowed(post.getGroupParticipationAllowed())
                .publicOfficialParticipationAllowed(post.getPublicOfficialParticipationAllowed())
                .familyParticipationAllowed(post.getFamilyParticipationAllowed())
                .hostOrganizationName(post.getHostOrganizationName())
                .managerName(post.getManagerName())
                .managerPhoneNumber(post.getManagerPhoneNumber())
                .managerFaxNumber(post.getManagerFaxNumber())
                .managerAddress(post.getManagerAddress())
                .managerEmail(post.getManagerEmail())
                .provinceCode(post.getProvinceCode())
                .districtCode(post.getDistrictCode())
                .activityAddress1(post.getActivityAddress1())
                .activityAddress2(post.getActivityAddress2())
                .activityAddress3(post.getActivityAddress3())
                .activityCoordinates1(post.getActivityCoordinates1())
                .activityCoordinates2(post.getActivityCoordinates2())
                .activityCoordinates3(post.getActivityCoordinates3())
                .build();
    }

    private static PostingInfoDTOBuilder summaryBuilder(Post post) {
        return PostingInfoDTO.builder()
                .id(post.getId())
                .programRegistrationNumber(post.getProgramRegistrationNumber())
                .programTitle(post.getProgramTitle())
                .registeringOrganizationName(post.getRegisteringOrganizationName())
                .activityPlace(post.getActivityPlace())
                .recruitmentStatus(post.getRecruitmentStatus())
                .programStartDate(post.getProgramStartDate())
                .programEndDate(post.getProgramEndDate())
                .recruitmentStartDate(post.getRecruitmentStartDate())
                .recruitmentEndDate(post.getRecruitmentEndDate())
                .recruitmentCapacity(post.getRecruitmentCapacity());
    }
}
