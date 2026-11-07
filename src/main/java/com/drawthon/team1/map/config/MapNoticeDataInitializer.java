package com.drawthon.team1.map.config;

import com.drawthon.team1.map.domain.MapNotice;
import com.drawthon.team1.map.domain.RecruitStatus;
import com.drawthon.team1.map.repository.MapNoticeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.List;

@Configuration
public class MapNoticeDataInitializer {

    @Bean
    CommandLineRunner initializeMapNotices(MapNoticeRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }

            repository.saveAll(List.of(
                    new MapNotice(
                            "한강공원 환경정화 봉사",
                            "서울시 자원봉사센터",
                            "서울특별시 영등포구 여의동로 330",
                            37.5285,
                            126.9349,
                            LocalDate.of(2026, 11, 1),
                            LocalDate.of(2026, 11, 15),
                            RecruitStatus.RECRUITING
                    ),
                    new MapNotice(
                            "서울숲 나무 돌봄 봉사",
                            "성동구 자원봉사센터",
                            "서울특별시 성동구 뚝섬로 273",
                            37.5444,
                            127.0374,
                            LocalDate.of(2026, 11, 3),
                            LocalDate.of(2026, 11, 20),
                            RecruitStatus.RECRUITING
                    ),
                    new MapNotice(
                            "해운대 해양정화 활동",
                            "부산광역시 자원봉사센터",
                            "부산광역시 해운대구 해운대해변로 264",
                            35.1587,
                            129.1604,
                            LocalDate.of(2026, 10, 1),
                            LocalDate.of(2026, 10, 31),
                            RecruitStatus.CLOSED
                    )
            ));
        };
    }
}
