package com.drawthon.team1.posting.config;

import com.drawthon.team1.posting.client.VolunteerApiClient;
import com.drawthon.team1.posting.dto.PostingExternalDTO;
import com.drawthon.team1.posting.repository.PostingRepository;
import com.drawthon.team1.posting.service.PostingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 서버가 시작될 때 1365 농어촌 봉사 공고(상위분야코드는 volunteer.api.upper-category-code)를 가져와 저장한다.
 * H2 메모리 DB는 재시작할 때마다 비기 때문에, 로그인할 때마다가 아니라 시작할 때 한 번만 가져온다.
 *
 * - 1365 호출이 느려도 서버 시작을 막지 않도록 백그라운드 스레드에서 실행한다.
 * - 인증키가 없거나 1365가 실패해도 서버는 정상으로 뜨고, 로그만 남긴다.
 * - 이미 저장된 공고는 건너뛴다. 공고 한 건이 실패해도 나머지는 계속 가져온다.
 * - 공공데이터포털은 요청을 연달아 보내면 429(Too Many Requests)로 막으므로, 공고 사이에 간격을 두고
 *   실패한 공고는 잠시 쉰 뒤 한 번 더 시도한다.
 * - volunteer.api.import-on-startup=false로 끌 수 있다 (테스트에서는 꺼 둔다).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "volunteer.api.import-on-startup", havingValue = "true", matchIfMissing = true)
public class VolunteerPostingInitializer implements ApplicationRunner {
    private final VolunteerApiClient volunteerApiClient;
    private final PostingService postingService;
    private final PostingRepository postingRepository;
    private final int importLimit;
    /** 공고 상세 요청 사이 간격(ms). 재시도 전에는 이 값의 10배를 쉰다. */
    private final long intervalMillis;

    public VolunteerPostingInitializer(VolunteerApiClient volunteerApiClient,
                                       PostingService postingService,
                                       PostingRepository postingRepository,
                                       @Value("${volunteer.api.import-limit:50}") int importLimit,
                                       @Value("${volunteer.api.import-interval-ms:300}") long intervalMillis) {
        this.volunteerApiClient = volunteerApiClient;
        this.postingService = postingService;
        this.postingRepository = postingRepository;
        this.importLimit = importLimit;
        this.intervalMillis = intervalMillis;
    }

    @Override
    public void run(ApplicationArguments args) {
        Thread.ofVirtual().name("volunteer-posting-import").start(this::importPostings);
    }

    /** 가져온 공고 수를 돌려준다. */
    int importPostings() {
        if (!volunteerApiClient.isConfigured()) {
            log.info("1365 인증키(VOLUNTEER_API_SERVICE_KEY)가 없어 1365 공고 자동 가져오기를 건너뜁니다.");
            return 0;
        }

        List<PostingExternalDTO> found;
        try {
            found = volunteerApiClient.search(null, 1, importLimit);
        } catch (RuntimeException e) {
            log.warn("1365 공고 검색에 실패해 자동 가져오기를 건너뜁니다: {}", e.getMessage());
            return 0;
        }

        List<String> targets = found.stream()
                .map(PostingExternalDTO::getProgramRegistrationNumber)
                .filter(number -> number != null && !postingRepository.existsByProgramRegistrationNumber(number))
                .toList();

        List<String> failed = importAll(targets);
        int imported = targets.size() - failed.size();
        if (!failed.isEmpty()) {
            log.info("1365 공고 {}건을 잠시 뒤 다시 가져옵니다.", failed.size());
            pause(intervalMillis * 10);
            List<String> stillFailed = importAll(failed);
            imported += failed.size() - stillFailed.size();
            stillFailed.forEach(number -> log.warn("1365 공고 {}를 끝내 가져오지 못했습니다.", number));
        }
        log.info("1365 공고 자동 가져오기 완료: 검색 {}건 중 {}건 저장", found.size(), imported);
        return imported;
    }

    /** 공고를 차례로 가져오고, 실패한 등록번호 목록을 돌려준다. */
    private List<String> importAll(List<String> numbers) {
        List<String> failed = new ArrayList<>();
        for (int i = 0; i < numbers.size(); i++) {
            if (i > 0) {
                pause(intervalMillis);
            }
            String number = numbers.get(i);
            try {
                postingService.importFromVolunteerApi(number);
            } catch (RuntimeException e) {
                log.warn("1365 공고 {} 가져오기 실패: {}", number, e.getMessage());
                failed.add(number);
            }
        }
        return failed;
    }

    private static void pause(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
