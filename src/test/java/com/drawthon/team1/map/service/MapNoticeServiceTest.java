package com.drawthon.team1.map.service;

import com.drawthon.team1.map.domain.MapNotice;
import com.drawthon.team1.map.domain.RecruitStatus;
import com.drawthon.team1.map.dto.MapMarkerResponse;
import com.drawthon.team1.map.dto.MapNoticeSummaryResponse;
import com.drawthon.team1.map.repository.MapNoticeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MapNoticeServiceTest {

    @Mock
    private MapNoticeRepository repository;

    @InjectMocks
    private MapNoticeService service;

    @Test
    void returnsMapMarkers() {
        MapNotice notice = createNotice();
        given(repository.findAll(any(Sort.class))).willReturn(List.of(notice));

        List<MapMarkerResponse> result = service.getMapNotices();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().title()).isEqualTo("한강공원 환경정화 봉사");
        assertThat(result.getFirst().latitude()).isEqualTo(37.5285);
        assertThat(result.getFirst().longitude()).isEqualTo(126.9349);
    }

    @Test
    void returnsSelectedNoticeSummary() {
        MapNotice notice = createNotice();
        given(repository.findById(1L)).willReturn(Optional.of(notice));

        MapNoticeSummaryResponse result = service.getNoticeSummary(1L);

        assertThat(result.title()).isEqualTo("한강공원 환경정화 봉사");
        assertThat(result.organization()).isEqualTo("서울시 자원봉사센터");
    }

    @Test
    void throwsNotFoundWhenNoticeDoesNotExist() {
        given(repository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getNoticeSummary(999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }

    private MapNotice createNotice() {
        return new MapNotice(
                "한강공원 환경정화 봉사",
                "서울시 자원봉사센터",
                "서울특별시 영등포구 여의동로 330",
                37.5285,
                126.9349,
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 15),
                RecruitStatus.RECRUITING
        );
    }
}
