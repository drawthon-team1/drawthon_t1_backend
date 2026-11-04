package com.drawthon.team1.posting.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 1365 검색 결과 dto
 * 관리자가 등록할 공고의 프로그램등록번호를 찾을 때 사용하며, 값은 1365 응답 그대로 전달한다.
 */

@Getter
@Builder
public class PostingExternalDTO {
    private final String programRegistrationNumber;
    private final String programTitle;
    private final String registeringOrganizationName;
    private final String programStartDate;
    private final String programEndDate;
    private final String recruitmentStatus;
    private final String activityStartTime;
    private final String activityEndTime;
    private final String recruitmentStartDate;
    private final String recruitmentEndDate;
    private final String activityPlace;
    private final String serviceCategoryCode;
    private final String adultParticipationAllowed;
    private final String youthParticipationAllowed;
    private final String provinceCode;
    private final String districtCode;
    private final String programDetailUrl;
}
