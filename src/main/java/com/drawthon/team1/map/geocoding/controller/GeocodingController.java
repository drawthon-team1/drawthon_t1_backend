package com.drawthon.team1.map.geocoding.controller;

import com.drawthon.team1.map.geocoding.dto.GeocodingResponse;
import com.drawthon.team1.map.geocoding.service.GeocodingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "지도 좌표 변환 API", description = "주소를 카카오맵 좌표로 변환합니다.")
@Validated
@RestController
@RequestMapping("/api/map/geocode")
public class GeocodingController {

    private final GeocodingService geocodingService;

    public GeocodingController(GeocodingService geocodingService) {
        this.geocodingService = geocodingService;
    }

    @Operation(summary = "주소를 위도·경도로 변환")
    @GetMapping
    public GeocodingResponse geocode(
            @Parameter(description = "좌표로 변환할 주소", example = "서울특별시 영등포구 여의동로 330")
            @RequestParam
            @NotBlank(message = "주소는 비어 있을 수 없습니다.")
            String address
    ) {
        return geocodingService.geocode(address);
    }
}
