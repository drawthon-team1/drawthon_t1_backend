package com.drawthon.team1.map.dto;

import com.drawthon.team1.map.domain.MapNotice;
import com.drawthon.team1.map.domain.RecruitStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "지도에서 선택한 공고의 요약정보")
public record MapNoticeSummaryResponse(
        @Schema(description = "공고 ID", example = "1")
        Long noticeId,

        @Schema(description = "공고 제목", example = "한강공원 환경정화 봉사")
        String title,

        @Schema(description = "주관 기관", example = "서울시 자원봉사센터")
        String organization,

        @Schema(description = "활동 주소", example = "서울특별시 영등포구 여의동로 330")
        String address,

        @Schema(description = "모집 시작일", example = "2026-11-01")
        LocalDate recruitStartDate,

        @Schema(description = "모집 종료일", example = "2026-11-15")
        LocalDate recruitEndDate,

        @Schema(description = "모집 상태", example = "RECRUITING")
        RecruitStatus status
) {
    public static MapNoticeSummaryResponse from(MapNotice notice) {
        return new MapNoticeSummaryResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getOrganization(),
                notice.getAddress(),
                notice.getRecruitStartDate(),
                notice.getRecruitEndDate(),
                notice.getStatus()
        );
    }
}
