package com.drawthon.team1.posting.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostingDraftRequest(
        @NotBlank(message = "공고 설명을 입력해 주세요.")
        @Size(max = 4000, message = "공고 설명은 4000자 이하여야 합니다.")
        String description
) {
}
