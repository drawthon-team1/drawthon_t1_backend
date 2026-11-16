package com.drawthon.team1.posting.ai.controller;

import com.drawthon.team1.posting.ai.dto.PostingDraftRequest;
import com.drawthon.team1.posting.ai.dto.PostingDraftResponse;
import com.drawthon.team1.posting.ai.service.PostingDraftService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/posting/ai")
@RequiredArgsConstructor
@Tag(name = "Posting AI Controller", description = "자연어 공고 초안 생성")
public class PostingAiController {
    private final PostingDraftService postingDraftService;

    @Operation(summary = "AI 공고 초안 생성",
            description = "자연어 설명을 공고 필드로 정리해 반환하며 DB에는 저장하지 않는다. "
                    + "missingFields는 실제 공고 등록 전에 입력해야 할 필수 필드다. "
                    + "reward는 WAGE일 때 일당(원), VOLUNTEER_HOURS일 때 총 봉사시간이다.\n\n"
                    + "권한: ADMIN")
    @PostMapping("/draft")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PostingDraftResponse> createDraft(@Valid @RequestBody PostingDraftRequest request) {
        return ResponseEntity.ok(postingDraftService.createDraft(request.description()));
    }
}
