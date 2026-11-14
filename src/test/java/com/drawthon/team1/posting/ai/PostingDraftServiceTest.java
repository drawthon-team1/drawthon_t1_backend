package com.drawthon.team1.posting.ai;

import com.drawthon.team1.posting.ai.dto.AiPostingDraft;
import com.drawthon.team1.posting.ai.dto.PostingDraftResponse;
import com.drawthon.team1.posting.ai.service.PostingDraftService;
import com.drawthon.team1.posting.entity.RewardType;
import com.drawthon.team1.posting.entity.WorkType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PostingDraftServiceTest {
    @Test
    void mapsPaidDraftAndReportsMissingRequiredFields() {
        AiPostingDraft ai = emptyDraft();
        ai.programTitle = Optional.of("  사과 수확 일손을 구합니다  ");
        ai.programStartDate = Optional.of("2030-10-10");
        ai.programEndDate = Optional.of("2030-10-11");
        ai.recruitmentCapacity = Optional.of(5);
        ai.rewardType = Optional.of(RewardType.WAGE);
        ai.reward = Optional.of(100_000);
        ai.workType = Optional.of(WorkType.HARVEST);

        PostingDraftResponse result = new PostingDraftService(ignored -> ai).createDraft("사과 수확");

        assertThat(result.programTitle()).isEqualTo("사과 수확 일손을 구합니다");
        assertThat(result.programStartDate()).isEqualTo(LocalDate.of(2030, 10, 10));
        assertThat(result.rewardType()).isEqualTo(RewardType.WAGE);
        assertThat(result.reward()).isEqualTo(100_000);
        assertThat(result.missingFields()).containsExactly("recruitmentStartDate", "recruitmentEndDate");
    }

    @Test
    void rejectsInvalidGeneratedValuesInsteadOfPassingThemToCreateApi() {
        AiPostingDraft ai = emptyDraft();
        ai.programStartDate = Optional.of("10월 10일");
        ai.activityStartTime = Optional.of("오전 아홉시");
        ai.recruitmentCapacity = Optional.of(0);
        ai.reward = Optional.of(-1);

        PostingDraftResponse result = new PostingDraftService(ignored -> ai).createDraft("공고");

        assertThat(result.programStartDate()).isNull();
        assertThat(result.activityStartTime()).isNull();
        assertThat(result.recruitmentCapacity()).isNull();
        assertThat(result.reward()).isNull();
        assertThat(result.warnings()).hasSize(4);
        assertThat(result.missingFields()).contains("programStartDate", "recruitmentCapacity", "reward");
    }

    private static AiPostingDraft emptyDraft() {
        AiPostingDraft draft = new AiPostingDraft();
        draft.programTitle = Optional.empty();
        draft.programContent = Optional.empty();
        draft.programStartDate = Optional.empty();
        draft.programEndDate = Optional.empty();
        draft.activityStartTime = Optional.empty();
        draft.activityEndTime = Optional.empty();
        draft.recruitmentStartDate = Optional.empty();
        draft.recruitmentEndDate = Optional.empty();
        draft.recruitmentCapacity = Optional.empty();
        draft.activityPlace = Optional.empty();
        draft.rewardType = Optional.empty();
        draft.reward = Optional.empty();
        draft.address = Optional.empty();
        draft.workType = Optional.empty();
        draft.warnings = List.of();
        return draft;
    }
}
