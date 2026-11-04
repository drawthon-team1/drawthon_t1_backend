package com.drawthon.team1.posting.controller;

import com.drawthon.team1.posting.dto.PostingCreateDTO;
import com.drawthon.team1.posting.dto.PostingExternalDTO;
import com.drawthon.team1.posting.dto.PostingInfoDTO;
import com.drawthon.team1.posting.dto.PostingResponseDTO;
import com.drawthon.team1.posting.dto.PostingUpdateDTO;
import com.drawthon.team1.posting.service.PostingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * TODO: 로그인 기능이 merge되면 경로의 {userId}를 인증된 사용자 정보로 교체한다.
 * 지금은 요청자가 userId를 직접 지정할 수 있어 권한 검사를 우회할 수 있다.
 */

@CrossOrigin(value = "http://localhost:3000")
@RestController
@RequestMapping("/api/v1/posting")
@RequiredArgsConstructor
public class PostingController {
    private final PostingService postingService;

    /**
     * 공고 목록 조회 (대시보드용 요약 정보, 최신 등록순)
     * 권한 : ADMIN, USER
     */
    @GetMapping
    public ResponseEntity<List<PostingInfoDTO>> getPostings() {
        return ResponseEntity.ok(postingService.getPostings());
    }

    /**
     * 단일 공고 조회 (내용, 담당자 연락처, 신청 인원을 포함한 전체 정보)
     * 권한 : ADMIN, USER
     */
    @GetMapping("/{postingId}")
    public ResponseEntity<PostingInfoDTO> getPosting(@PathVariable Long postingId) {
        return ResponseEntity.ok(postingService.getPosting(postingId));
    }

    /**
     * 1365 공고 검색 (우리 DB가 아닌 1365에서 직접 조회)
     * 관리자가 공고 생성에 필요한 프로그램등록번호를 찾을 때 사용한다.
     * 권한 : ADMIN, USER
     */
    @GetMapping("/external")
    public ResponseEntity<List<PostingExternalDTO>> searchExternalPostings(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) int pageNo,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int numOfRows) {
        return ResponseEntity.ok(postingService.searchExternalPostings(keyword, pageNo, numOfRows));
    }

    /**
     * 공고 생성 (프로그램등록번호로 1365 상세 정보를 가져와 우리 DB에 저장)
     * 권한 : ADMIN
     */
    @PostMapping("/{userId}")
    public ResponseEntity<PostingResponseDTO> createPosting(@PathVariable Long userId,
                                                            @Valid @RequestBody PostingCreateDTO postingCreateDTO) {
        return ResponseEntity.ok(postingService.createPosting(userId, postingCreateDTO));
    }

    /**
     * 공고 수정 (우리 DB의 공고만 수정되며 1365에는 반영되지 않음)
     * 권한 : ADMIN
     */
    @PutMapping("/{userId}/{postingId}")
    public ResponseEntity<PostingResponseDTO> updatePosting(@PathVariable Long userId,
                                                            @PathVariable Long postingId,
                                                            @Valid @RequestBody PostingUpdateDTO postingUpdateDTO) {
        return ResponseEntity.ok(postingService.updatePosting(userId, postingId, postingUpdateDTO));
    }

    /**
     * 공고 삭제 (해당 공고의 신청 내역도 함께 삭제, 1365에는 반영되지 않음)
     * 권한 : ADMIN
     */
    @DeleteMapping("/{userId}/{postingId}")
    public ResponseEntity<PostingResponseDTO> deletePosting(@PathVariable Long userId, @PathVariable Long postingId) {
        return ResponseEntity.ok(postingService.deletePosting(userId, postingId));
    }

    /**
     * 공고 신청 (모집 기간, 모집 인원, 중복 신청을 확인하며 1365에는 신청되지 않음)
     * 권한 : USER
     */
    @PostMapping("/{userId}/{postingId}/me")
    public ResponseEntity<PostingResponseDTO> applyPosting(@PathVariable Long userId, @PathVariable Long postingId) {
        return ResponseEntity.ok(postingService.applyPosting(userId, postingId));
    }

    /**
     * 공고 신청 철회
     * 권한 : USER
     */
    @DeleteMapping("/{userId}/{postingId}/me")
    public ResponseEntity<PostingResponseDTO> withdrawPosting(@PathVariable Long userId, @PathVariable Long postingId) {
        return ResponseEntity.ok(postingService.withdrawPosting(userId, postingId));
    }
}
