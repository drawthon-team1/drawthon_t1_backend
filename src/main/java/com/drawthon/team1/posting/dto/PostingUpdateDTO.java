package com.drawthon.team1.posting.dto;

import com.drawthon.team1.posting.entity.WorkType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 공고 모집 수정 dto
 * PUT 요청이므로 수정 가능한 항목 전체를 보내야 한다.
 */

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostingUpdateDTO {

    @NotBlank
    @Size(max = 500)
    private String programTitle;

    private String programContent;

    @NotNull
    private LocalDate programStartDate;

    @NotNull
    private LocalDate programEndDate;

    @Size(max = 255)
    private String activityStartTime;

    @Size(max = 255)
    private String activityEndTime;

    @NotNull
    private LocalDate recruitmentStartDate;

    @NotNull
    private LocalDate recruitmentEndDate;

    @NotNull
    @Positive
    private Integer recruitmentCapacity;

    @Size(max = 500)
    private String activityPlace;

    /** 임금(원). 직접 작성한 공고만 반영하고, 생략하면 기존 값을 유지한다. 1365 공고는 활동 시간으로 다시 계산한다 */
    @Positive
    private Integer reward;

    /** 활동 주소 (지역 필터에 사용). 생략하면 기존 값을 유지한다 */
    @Size(max = 500)
    private String address;

    /** 작업 종류. 생략하면 기존 값을 유지한다 */
    private WorkType workType;

    @AssertTrue(message = "봉사종료일자는 봉사시작일자보다 빠를 수 없습니다.")
    public boolean isProgramPeriodValid() {
        return programStartDate == null || programEndDate == null || !programEndDate.isBefore(programStartDate);
    }

    @AssertTrue(message = "모집종료일은 모집시작일보다 빠를 수 없습니다.")
    public boolean isRecruitmentPeriodValid() {
        return recruitmentStartDate == null || recruitmentEndDate == null || !recruitmentEndDate.isBefore(recruitmentStartDate);
    }
}
