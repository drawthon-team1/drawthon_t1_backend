package com.drawthon.team1.map.service;

import com.drawthon.team1.map.domain.MapNotice;
import com.drawthon.team1.map.dto.MapMarkerResponse;
import com.drawthon.team1.map.dto.MapNoticeSummaryResponse;
import com.drawthon.team1.map.repository.MapNoticeRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MapNoticeService {

    private final MapNoticeRepository repository;

    public MapNoticeService(MapNoticeRepository repository) {
        this.repository = repository;
    }

    public List<MapMarkerResponse> getMapNotices() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(MapMarkerResponse::from)
                .toList();
    }

    public MapNoticeSummaryResponse getNoticeSummary(Long noticeId) {
        MapNotice notice = repository.findById(noticeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "공고를 찾을 수 없습니다. noticeId=" + noticeId
                ));

        return MapNoticeSummaryResponse.from(notice);
    }
}
