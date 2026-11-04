package com.drawthon.team1.map.dto;

import com.drawthon.team1.map.domain.MapNotice;
import com.drawthon.team1.map.domain.RecruitStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "지도에 표시할 공고 마커 정보")
public record MapMarkerResponse(
        @Schema(description = "공고 ID", example = "1")
        Long noticeId,

        @Schema(description = "공고 제목", example = "한강공원 환경정화 봉사")
        String title,

        @Schema(description = "위도", example = "37.5285")
        Double latitude,

        @Schema(description = "경도", example = "126.9349")
        Double longitude,

        @Schema(description = "모집 상태", example = "RECRUITING")
        RecruitStatus status
) {
    public static MapMarkerResponse from(MapNotice notice) {
        return new MapMarkerResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getLatitude(),
                notice.getLongitude(),
                notice.getStatus()
        );
    }
}
