package com.drawthon.team1.posting.controller;

import com.drawthon.team1.posting.dto.DurationRange;
import com.drawthon.team1.posting.dto.PostingCreateDTO;
import com.drawthon.team1.posting.dto.PostingExternalDTO;
import com.drawthon.team1.posting.dto.PostingFilter;
import com.drawthon.team1.posting.dto.PostingImportDTO;
import com.drawthon.team1.posting.dto.PostingInfoDTO;
import com.drawthon.team1.posting.dto.PostingResponseDTO;
import com.drawthon.team1.posting.dto.PostingUpdateDTO;
import com.drawthon.team1.posting.dto.TimeSlot;
import com.drawthon.team1.posting.entity.RecruitmentStatus;
import com.drawthon.team1.posting.entity.RewardType;
import com.drawthon.team1.posting.entity.WorkType;
import com.drawthon.team1.posting.service.PostingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 모든 공고 API는 로그인이 필요하다.
 * 요청한 회원은 경로가 아니라 로그인 세션(@AuthenticationPrincipal)에서 가져와, 다른 회원으로 위장할 수 없다.
 * CORS 허용 주소는 SecurityConfig의 전역 설정(corsConfigurationSource)에서 한 곳으로 관리한다.
 */

@RestController
@RequestMapping("/api/v1/posting")
@RequiredArgsConstructor
@Tag(name = "Posting Controller", description = "통합 공고 컨트롤러")
public class PostingController {
    private final PostingService postingService;

    @Operation(summary = "공고 목록 조회",
            description = "대시보드용 요약 정보(활동 시간, 신청 인원 포함)를 최신 등록순으로 반환한다. 상세 설명은 단건 조회에서 확인한다.\n\n"
                    + "필터: 값을 보내지 않은 조건은 적용하지 않는다. 서로 다른 조건은 모두 만족해야 하고(AND), "
                    + "한 조건에 여러 값을 보내면 하나만 맞아도 된다(OR). 여러 값은 durations=ONE_DAY,UP_TO_3_DAYS처럼 쉼표로 보낸다.\n\n"
                    + "권한: 로그인한 회원 (ADMIN, USER)\n\n"
                    + "실패: 400 잘못된 필터 값, 일정 마감일이 시작일보다 빠름")
    @GetMapping
    public ResponseEntity<List<PostingInfoDTO>> getPostings(
            @Parameter(description = "검색어. 공고 제목, 등록 기관, 봉사 장소, 주소 중 하나에 포함되면 해당 (대소문자 무시)", example = "수확")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "공고 유형. VOLUNTEER_HOURS = 봉사, WAGE = 유급. 생략하면 전체")
            @RequestParam(required = false) RewardType rewardType,
            @Parameter(description = "지역: 시·군(구). 공고 주소에 이 이름이 있어야 한다", example = "포천시")
            @RequestParam(required = false) String sigungu,
            @Parameter(description = "지역: 읍·면·동. 공고 주소에 이 이름이 있어야 한다", example = "군내면")
            @RequestParam(required = false) String eupMyeonDong,
            @Parameter(description = "일정 시작일. 봉사 기간이 이 일정과 하루라도 겹치는 공고", example = "2026-11-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "일정 마감일", example = "2026-11-30")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "기간(봉사 종료일 - 시작일 + 1일). ONE_DAY 하루 / UP_TO_3_DAYS 3일 이하 / UP_TO_1_WEEK 일주일 이하 / "
                    + "TWO_WEEKS_TO_1_MONTH 2주~1개월 미만 / ONE_MONTH_OR_MORE 1개월 이상")
            @RequestParam(required = false) List<DurationRange> durations,
            @Parameter(description = "시간대(활동 시간과 겹치면 해당). DAWN 새벽 0~6시 / MORNING 오전 6~12시 / AFTERNOON 오후 12~18시 / "
                    + "EVENING 저녁 18~22시 / NIGHT 야간 22~24시")
            @RequestParam(required = false) List<TimeSlot> timeSlots,
            @Parameter(description = "모집 상태. RECRUITING 모집 중(마감 직전 포함) / CLOSING_SOON 마감 직전(모집 종료 3일 이내) / "
                    + "CLOSED 모집 완료(기간 종료 또는 인원 마감) / UPCOMING 모집 시작 전")
            @RequestParam(required = false) List<RecruitmentStatus> statuses,
            @Parameter(description = "작업 종류. HARVEST 수확 / SORTING_PACKING 선별·포장 / PLANTING_CULTIVATION 파종·재배 / WEEDING 제초 / ETC 기타")
            @RequestParam(required = false) List<WorkType> workTypes) {
        PostingFilter filter = new PostingFilter(keyword, rewardType, sigungu, eupMyeonDong, startDate, endDate,
                durations, timeSlots, statuses, workTypes);
        return ResponseEntity.ok(postingService.getPostings(filter));
    }

    @Operation(summary = "공고 단건 조회",
            description = "내용, 활동 시간, 이 서비스의 신청 인원을 포함한 전체 정보를 반환한다.\n\n"
                    + "권한: 로그인한 회원 (ADMIN, USER)\n\n"
                    + "실패: 404 공고 없음")
    @GetMapping("/{postingId}")
    public ResponseEntity<PostingInfoDTO> getPosting(
            @Parameter(description = "조회할 공고 ID", example = "1") @PathVariable Long postingId) {
        return ResponseEntity.ok(postingService.getPosting(postingId));
    }

    @Operation(summary = "1365 공고 검색",
            description = "우리 DB가 아니라 1365에서 직접 한 페이지씩 조회한다. 결과는 저장되지 않는다. "
                    + "농어촌 봉사 분야(상위분야코드 0600) 공고만 조회한다. "
                    + "관리자가 1365 공고 등록에 쓸 프로그램등록번호를 찾을 때 사용한다.\n\n"
                    + "권한: 로그인한 회원 (ADMIN, USER)\n\n"
                    + "실패: 400 페이지 파라미터 범위 오류, 502 1365 호출 실패, 503 1365 인증키 미설정")
    @GetMapping("/external")
    public ResponseEntity<List<PostingExternalDTO>> searchExternalPostings(
            @Parameter(description = "검색어. 비우면 조건 없이 목록을 조회한다", example = "환경")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "페이지 번호 (1 이상)", example = "1")
            @RequestParam(defaultValue = "1") @Min(1) int pageNo,
            @Parameter(description = "페이지당 건수 (1~100)", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int numOfRows) {
        return ResponseEntity.ok(postingService.searchExternalPostings(keyword, pageNo, numOfRows));
    }

    @Operation(summary = "공고 직접 생성",
            description = "1365와 연동되지 않은 공고를 작성해 저장한다. rewardType은 WAGE(유급) 또는 VOLUNTEER_HOURS(봉사시간)이며, "
                    + "reward에는 각각 일당(원) 또는 총 봉사시간을 넣는다. "
                    + "프로그램등록번호와 등록 기관은 비어 있다.\n\n"
                    + "권한: ADMIN\n\n"
                    + "실패: 400 입력 오류(필수값 누락, 종료일이 시작일보다 빠름), 403 권한 없음")
    @PostMapping
    public ResponseEntity<PostingResponseDTO> createPosting(@Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
                                                            @Valid @RequestBody PostingCreateDTO postingCreateDTO) {
        return ResponseEntity.ok(postingService.createPosting(memberId, postingCreateDTO));
    }

    @Operation(summary = "1365 공고 등록",
            description = "프로그램등록번호로 1365 상세 정보를 가져와 우리 DB에 저장한다. "
                    + "리워드는 봉사시간(rewardType = VOLUNTEER_HOURS)이며 reward는 1365 활동 종료시간 - 시작시간(시간)으로 계산한다.\n\n"
                    + "권한: ADMIN\n\n"
                    + "실패: 400 등록번호 누락, 403 권한 없음, 404 1365에 없는 공고, 409 이미 등록된 공고, 502 1365 호출 실패")
    @PostMapping("/external")
    public ResponseEntity<PostingResponseDTO> importPosting(@Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
                                                            @Valid @RequestBody PostingImportDTO postingImportDTO) {
        return ResponseEntity.ok(postingService.importPosting(memberId, postingImportDTO));
    }

    @Operation(summary = "공고 수정",
            description = "우리 DB의 공고만 수정되며 1365에는 반영되지 않는다. PUT이므로 수정 가능한 항목을 모두 보내야 한다. "
                    + "직접 작성한 공고는 보낸 rewardType과 reward로 바뀐다. "
                    + "1365에서 가져온 공고는 보낸 rewardType과 reward를 무시하고, 봉사시간을 수정한 활동 시간으로 다시 계산한다.\n\n"
                    + "권한: ADMIN\n\n"
                    + "실패: 400 입력 오류, 403 권한 없음, 404 공고 없음")
    @PutMapping("/{postingId}")
    public ResponseEntity<PostingResponseDTO> updatePosting(@Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
                                                            @Parameter(description = "수정할 공고 ID", example = "1") @PathVariable Long postingId,
                                                            @Valid @RequestBody PostingUpdateDTO postingUpdateDTO) {
        return ResponseEntity.ok(postingService.updatePosting(memberId, postingId, postingUpdateDTO));
    }

    @Operation(summary = "공고 삭제",
            description = "공고와 그 공고의 신청 내역을 함께 삭제하고, 삭제된 공고 정보를 반환한다. 1365에는 반영되지 않는다.\n\n"
                    + "권한: ADMIN\n\n"
                    + "실패: 403 권한 없음, 404 공고 없음")
    @DeleteMapping("/{postingId}")
    public ResponseEntity<PostingResponseDTO> deletePosting(@Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
                                                            @Parameter(description = "삭제할 공고 ID", example = "1") @PathVariable Long postingId) {
        return ResponseEntity.ok(postingService.deletePosting(memberId, postingId));
    }

    @Operation(summary = "공고 신청",
            description = "로그인한 회원으로 공고에 신청한다. 모집 기간, 모집 인원, 중복 신청을 확인하며 1365에는 신청되지 않는다.\n\n"
                    + "권한: USER\n\n"
                    + "실패: 403 권한 없음, 404 공고 없음, 409 이미 신청함·모집 기간 아님·모집 인원 마감")
    @PostMapping("/{postingId}/application")
    public ResponseEntity<PostingResponseDTO> applyPosting(@Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
                                                           @Parameter(description = "신청할 공고 ID", example = "1") @PathVariable Long postingId) {
        return ResponseEntity.ok(postingService.applyPosting(memberId, postingId));
    }

    @Operation(summary = "공고 신청 철회",
            description = "로그인한 회원의 신청 내역을 삭제한다. 철회 후 다시 신청할 수 있다.\n\n"
                    + "권한: USER\n\n"
                    + "실패: 403 권한 없음, 404 공고 없음·신청 내역 없음")
    @DeleteMapping("/{postingId}/application")
    public ResponseEntity<PostingResponseDTO> withdrawPosting(@Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
                                                              @Parameter(description = "철회할 공고 ID", example = "1") @PathVariable Long postingId) {
        return ResponseEntity.ok(postingService.withdrawPosting(memberId, postingId));
    }
}
