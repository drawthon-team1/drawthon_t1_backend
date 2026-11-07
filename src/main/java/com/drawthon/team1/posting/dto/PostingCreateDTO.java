package com.drawthon.team1.posting.dto;

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
 * 공고 직접 생성 dto
 * 1365와 연동되지 않은 공고를 우리 서비스에서 직접 작성한다.
 */

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostingCreateDTO {

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

    @AssertTrue(message = "봉사종료일자는 봉사시작일자보다 빠를 수 없습니다.")
    public boolean isProgramPeriodValid() {
        return programStartDate == null || programEndDate == null || !programEndDate.isBefore(programStartDate);
    }

    @AssertTrue(message = "모집종료일은 모집시작일보다 빠를 수 없습니다.")
    public boolean isRecruitmentPeriodValid() {
        return recruitmentStartDate == null || recruitmentEndDate == null || !recruitmentEndDate.isBefore(recruitmentStartDate);
    }
}
