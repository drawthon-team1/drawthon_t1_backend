package com.drawthon.team1.posting.ai.client;

import com.drawthon.team1.posting.ai.dto.AiPostingDraft;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.JsonSchemaLocalValidation;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponse;
import com.openai.models.responses.StructuredResponseCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDate;

@Component
public class OpenAiPostingDraftClient implements PostingDraftGenerator {
    private final String apiKey;
    private final String model;

    public OpenAiPostingDraftClient(@Value("${openai.api-key:}") String apiKey,
                                    @Value("${openai.model:gpt-4o-mini}") String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public AiPostingDraft generate(String description) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "OpenAI API 키가 설정되지 않았습니다.");
        }

        OpenAIClient client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .timeout(Duration.ofSeconds(30))
                .maxRetries(2)
                .build();

        try {
            StructuredResponseCreateParams<AiPostingDraft> params = ResponseCreateParams.builder()
                    .model(model)
                    .instructions(systemInstructions())
                    .input(description)
                    .text(AiPostingDraft.class, JsonSchemaLocalValidation.NO)
                    .build();
            StructuredResponse<AiPostingDraft> response = client.responses().create(params);
            return response.output().stream()
                    .flatMap(item -> item.message().stream())
                    .flatMap(message -> message.content().stream())
                    .flatMap(content -> content.outputText().stream())
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                            "AI가 공고 초안을 반환하지 않았습니다."));
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "AI 공고 초안 생성에 실패했습니다.");
        }
    }

    private String systemInstructions() {
        return """
                당신은 60대 이상 사용자가 적은 한국어 메모를 정돈된 농촌 일자리/봉사 공고 초안으로 바꾸는 도우미다.
                오늘 날짜는 %s이다.
                입력에 없는 사실, 날짜, 시간, 장소, 인원, 금액을 추측하지 말고 해당 Optional 필드는 비워라.
                programTitle은 짧고 명확하게, programContent는 쉬운 존댓말과 짧은 문장으로 작성하라.
                날짜는 YYYY-MM-DD, 시간은 HH:mm 형식으로만 작성하라.
                rewardType은 유급이면 WAGE, 봉사이면 VOLUNTEER_HOURS다.
                WAGE의 reward는 일당 원 단위 정수이고, VOLUNTEER_HOURS의 reward는 전체 봉사시간 정수다.
                시급·월급만 주어져 일당을 알 수 없으면 reward를 비우고 warnings에 이유를 한국어로 남겨라.
                workType은 HARVEST, SORTING_PACKING, PLANTING_CULTIVATION, WEEDING, ETC 중 하나다.
                warnings에는 애매하거나 확인이 필요한 내용만 간결하게 담아라.
                """.formatted(LocalDate.now());
    }
}
