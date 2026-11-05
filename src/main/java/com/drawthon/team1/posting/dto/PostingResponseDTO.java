package com.drawthon.team1.posting.dto;

import com.drawthon.team1.posting.entity.Post;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 공고 모집 공통 반환 dto
 */

@Getter
@Builder
public class PostingResponseDTO {
    private final Long id;
    private final String programRegistrationNumber;
    private final String programTitle;
    private final String programContent;
    private final String recruitmentStatus;
    private final LocalDate programStartDate;
    private final LocalDate programEndDate;
    private final String activityStartTime;
    private final String activityEndTime;
    private final LocalDate recruitmentStartDate;
    private final LocalDate recruitmentEndDate;
    private final Integer recruitmentCapacity;
    private final Integer externalApplicantCount;
    /** 이 서비스에서 신청한 인원 */
    private final long applicantCount;
    private final String activityWeekdays;
    private final String serviceCategoryCode;
    private final String adultParticipationAllowed;
    private final String youthParticipationAllowed;
    private final String groupParticipationAllowed;
    private final String publicOfficialParticipationAllowed;
    private final String familyParticipationAllowed;
    private final String hostOrganizationName;
    private final String registeringOrganizationName;
    private final String activityPlace;
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

    public static PostingResponseDTO from(Post post, long applicantCount) {
        return PostingResponseDTO.builder()
                .id(post.getId())
                .programRegistrationNumber(post.getProgramRegistrationNumber())
                .programTitle(post.getProgramTitle())
                .programContent(post.getProgramContent())
                .recruitmentStatus(post.getRecruitmentStatus())
                .programStartDate(post.getProgramStartDate())
                .programEndDate(post.getProgramEndDate())
                .activityStartTime(post.getActivityStartTime())
                .activityEndTime(post.getActivityEndTime())
                .recruitmentStartDate(post.getRecruitmentStartDate())
                .recruitmentEndDate(post.getRecruitmentEndDate())
                .recruitmentCapacity(post.getRecruitmentCapacity())
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
                .registeringOrganizationName(post.getRegisteringOrganizationName())
                .activityPlace(post.getActivityPlace())
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
}
