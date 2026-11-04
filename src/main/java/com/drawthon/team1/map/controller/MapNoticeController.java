package com.drawthon.team1.map.controller;

import com.drawthon.team1.map.dto.MapMarkerResponse;
import com.drawthon.team1.map.dto.MapNoticeSummaryResponse;
import com.drawthon.team1.map.service.MapNoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "지도 공고 API", description = "지도 마커와 선택한 공고의 요약정보를 조회합니다.")
@RestController
@RequestMapping("/api/map/notices")
public class MapNoticeController {

    private final MapNoticeService mapNoticeService;

    public MapNoticeController(MapNoticeService mapNoticeService) {
        this.mapNoticeService = mapNoticeService;
    }

    @Operation(summary = "지도 공고 마커 목록 조회")
    @GetMapping
    public List<MapMarkerResponse> getMapNotices() {
        return mapNoticeService.getMapNotices();
    }

    @Operation(summary = "선택한 공고 요약정보 조회")
    @GetMapping("/{noticeId}")
    public MapNoticeSummaryResponse getNoticeSummary(
            @Parameter(description = "조회할 공고 ID", example = "1")
            @PathVariable Long noticeId
    ) {
        return mapNoticeService.getNoticeSummary(noticeId);
    }
}
