package com.drawthon.team1.posting.ai.client;

import com.drawthon.team1.posting.ai.dto.AiPostingDraft;

public interface PostingDraftGenerator {
    AiPostingDraft generate(String description);
}
