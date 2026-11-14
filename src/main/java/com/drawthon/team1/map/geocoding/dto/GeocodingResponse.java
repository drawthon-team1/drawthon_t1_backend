package com.drawthon.team1.map.geocoding.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주소를 변환한 지도 좌표")
public record GeocodingResponse(
        @Schema(description = "검색된 주소", example = "서울특별시 영등포구 여의동로 330")
        String address,

        @Schema(description = "위도", example = "37.5285")
        Double latitude,

        @Schema(description = "경도", example = "126.9349")
        Double longitude
) {
}
