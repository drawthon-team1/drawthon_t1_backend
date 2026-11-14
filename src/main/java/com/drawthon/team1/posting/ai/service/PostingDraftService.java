package com.drawthon.team1.posting.ai.service;

import com.drawthon.team1.posting.ai.client.PostingDraftGenerator;
import com.drawthon.team1.posting.ai.dto.AiPostingDraft;
import com.drawthon.team1.posting.ai.dto.PostingDraftResponse;
import com.drawthon.team1.posting.entity.RewardType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class PostingDraftService {
    private static final String TIME_PATTERN = "(?:[01]\\d|2[0-3]):[0-5]\\d";

    private final PostingDraftGenerator postingDraftGenerator;

    public PostingDraftResponse createDraft(String description) {
        AiPostingDraft ai = postingDraftGenerator.generate(description);
        List<String> warnings = ai.warnings == null ? new ArrayList<>() : new ArrayList<>(ai.warnings);

        String title = value(ai.programTitle, PostingDraftService::clean);
        String content = value(ai.programContent, PostingDraftService::clean);
        LocalDate programStart = parseDate(ai.programStartDate, "programStartDate", warnings);
        LocalDate programEnd = parseDate(ai.programEndDate, "programEndDate", warnings);
        String activityStart = parseTime(ai.activityStartTime, "activityStartTime", warnings);
        String activityEnd = parseTime(ai.activityEndTime, "activityEndTime", warnings);
        LocalDate recruitmentStart = parseDate(ai.recruitmentStartDate, "recruitmentStartDate", warnings);
        LocalDate recruitmentEnd = parseDate(ai.recruitmentEndDate, "recruitmentEndDate", warnings);
        Integer capacity = positive(value(ai.recruitmentCapacity, Function.identity()),
                "recruitmentCapacity", warnings);
        RewardType rewardType = value(ai.rewardType, Function.identity());
        Integer reward = positive(value(ai.reward, Function.identity()), "reward", warnings);

        validatePeriod(programStart, programEnd, "활동 종료일이 시작일보다 빠릅니다.", warnings);
        validatePeriod(recruitmentStart, recruitmentEnd, "모집 종료일이 시작일보다 빠릅니다.", warnings);

        List<String> missingFields = new ArrayList<>();
        addMissing(missingFields, "programTitle", title);
        addMissing(missingFields, "programStartDate", programStart);
        addMissing(missingFields, "programEndDate", programEnd);
        addMissing(missingFields, "recruitmentStartDate", recruitmentStart);
        addMissing(missingFields, "recruitmentEndDate", recruitmentEnd);
        addMissing(missingFields, "recruitmentCapacity", capacity);
        addMissing(missingFields, "rewardType", rewardType);
        addMissing(missingFields, "reward", reward);

        return new PostingDraftResponse(
                title,
                content,
                programStart,
                programEnd,
                activityStart,
                activityEnd,
                recruitmentStart,
                recruitmentEnd,
                capacity,
                value(ai.activityPlace, PostingDraftService::clean),
                rewardType,
                reward,
                value(ai.address, PostingDraftService::clean),
                value(ai.workType, Function.identity()),
                List.copyOf(missingFields),
                List.copyOf(warnings)
        );
    }

    private static LocalDate parseDate(Optional<String> date, String field, List<String> warnings) {
        String text = value(date, PostingDraftService::clean);
        if (text == null) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException exception) {
            warnings.add(field + "의 날짜 형식을 확인해 주세요.");
            return null;
        }
    }

    private static String parseTime(Optional<String> time, String field, List<String> warnings) {
        String text = value(time, PostingDraftService::clean);
        if (text == null || text.matches(TIME_PATTERN)) {
            return text;
        }
        warnings.add(field + "의 시간 형식을 확인해 주세요.");
        return null;
    }

    private static Integer positive(Integer number, String field, List<String> warnings) {
        if (number == null || number > 0) {
            return number;
        }
        warnings.add(field + "는 0보다 커야 합니다.");
        return null;
    }

    private static void validatePeriod(LocalDate start, LocalDate end, String message, List<String> warnings) {
        if (start != null && end != null && end.isBefore(start)) {
            warnings.add(message);
        }
    }

    private static void addMissing(List<String> missing, String field, Object value) {
        if (value == null) {
            missing.add(field);
        }
    }

    private static String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static <T, R> R value(Optional<T> optional, Function<T, R> mapper) {
        return optional == null ? null : optional.map(mapper).orElse(null);
    }
}
