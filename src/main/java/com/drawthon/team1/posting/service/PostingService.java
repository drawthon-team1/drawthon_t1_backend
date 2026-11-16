package com.drawthon.team1.posting.service;

import com.drawthon.team1.entity.Member;
import com.drawthon.team1.entity.MemberRole;
import com.drawthon.team1.posting.client.VolunteerApiClient;
import com.drawthon.team1.posting.dto.PostingCreateDTO;
import com.drawthon.team1.posting.dto.PostingExternalDTO;
import com.drawthon.team1.posting.dto.PostingFilter;
import com.drawthon.team1.posting.dto.PostingImportDTO;
import com.drawthon.team1.posting.dto.PostingInfoDTO;
import com.drawthon.team1.posting.dto.PostingResponseDTO;
import com.drawthon.team1.posting.dto.PostingUpdateDTO;
import com.drawthon.team1.posting.entity.Post;
import com.drawthon.team1.posting.entity.PostApplication;
import com.drawthon.team1.posting.repository.PostApplicationRepository;
import com.drawthon.team1.posting.repository.PostApplicationRepository.PostApplicantCount;
import com.drawthon.team1.posting.repository.PostingRepository;
import com.drawthon.team1.sessionlogin.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 공고 읽어오기 -> 공고 목록 대시보드 반환 : 공통
 * 공고 등록 / 수정 / 삭제 : 관리자(ADMIN)
 * 공고 신청 / 철회 : 사용자(USER)
 * memberId는 컨트롤러가 로그인 세션에서 꺼내 넘긴다. role은 매 요청마다 DB의 회원 정보로 확인한다.
 * => role을 mvp단계에서 두 개만 파도 기능 구현에 크게 문제가 발생하지 않을 것 같음.
 *
 * @Param : input custom DTO (convention : domain + function + "DTO")
 * @Return
 *  - 읽어오기 : dto list
 *  - 단일 공고 읽어오기 : info dto
 *  - 등록 / 수정 / 삭제 : response dto
 *  - 공고 신청 / 철회 : response dto
 */

@Service
@RequiredArgsConstructor
public class PostingService {
    private static final String DUPLICATE_POSTING_MESSAGE = "이미 등록된 공고입니다.";

    private final PostingRepository postingRepository;
    private final PostApplicationRepository postApplicationRepository;
    private final MemberRepository memberRepository;
    private final VolunteerApiClient volunteerApiClient;

    /**
     * 모집 상태는 저장된 값이 아니라 신청 인원과 오늘 날짜로 계산하므로, 공고를 모두 읽은 뒤 메모리에서 거른다.
     * MVP 규모(공고 수십~수백 건)를 전제로 한다.
     */
    @Transactional(readOnly = true)
    public List<PostingInfoDTO> getPostings(PostingFilter filter) {
        if (filter.startDate() != null && filter.endDate() != null && filter.endDate().isBefore(filter.startDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "일정 마감일은 시작일보다 빠를 수 없습니다.");
        }

        LocalDate today = LocalDate.now();
        Map<Long, Long> applicantCounts = postApplicationRepository.countGroupByPost().stream()
                .collect(Collectors.toMap(PostApplicantCount::getPostId, PostApplicantCount::getApplicantCount));

        return postingRepository.findAllByOrderByIdDesc().stream()
                .filter(post -> {
                    long applicantCount = applicantCounts.getOrDefault(post.getId(), 0L);
                    return filter.matches(post, post.recruitmentStatus(today, applicantCount));
                })
                .map(post -> {
                    long applicantCount = applicantCounts.getOrDefault(post.getId(), 0L);
                    return PostingInfoDTO.from(post, applicantCount, post.recruitmentStatus(today, applicantCount));
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public PostingInfoDTO getPosting(Long postingId) {
        Post post = postingRepository.findById(postingId).orElseThrow(PostingService::postingNotFound);
        long applicantCount = postApplicationRepository.countByPostId(postingId);
        return PostingInfoDTO.detailFrom(post, applicantCount, post.recruitmentStatus(LocalDate.now(), applicantCount));
    }

    public List<PostingExternalDTO> searchExternalPostings(String keyword, int pageNo, int numOfRows) {
        return volunteerApiClient.search(keyword, pageNo, numOfRows);
    }

    @Transactional
    public PostingResponseDTO createPosting(Long memberId, PostingCreateDTO postingCreateDTO) {
        requireRole(memberId, MemberRole.ADMIN);

        return PostingResponseDTO.from(postingRepository.save(Post.from(postingCreateDTO)), 0);
    }

    /**
     * 외부 api 호출 동안 DB 커넥션을 점유하지 않도록 트랜잭션을 걸지 않는다.
     */
    public PostingResponseDTO importPosting(Long memberId, PostingImportDTO postingImportDTO) {
        requireRole(memberId, MemberRole.ADMIN);
        return importFromVolunteerApi(postingImportDTO.getProgramRegistrationNumber());
    }

    /**
     * 프로그램등록번호로 1365 상세를 가져와 저장한다. 권한 확인은 호출하는 쪽에서 한다.
     * (관리자 API, 서버 시작 시 자동 가져오기)
     */
    public PostingResponseDTO importFromVolunteerApi(String programRegistrationNumber) {
        if (postingRepository.existsByProgramRegistrationNumber(programRegistrationNumber)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, DUPLICATE_POSTING_MESSAGE);
        }

        Post post = volunteerApiClient.fetchDetail(programRegistrationNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "1365에서 해당 공고를 찾을 수 없습니다."));
        try {
            return PostingResponseDTO.from(postingRepository.save(post), 0);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, DUPLICATE_POSTING_MESSAGE);
        }
    }

    @Transactional
    public PostingResponseDTO updatePosting(Long memberId, Long postingId, PostingUpdateDTO postingUpdateDTO) {
        requireRole(memberId, MemberRole.ADMIN);

        Post post = postingRepository.findById(postingId).orElseThrow(PostingService::postingNotFound);
        post.update(postingUpdateDTO);
        return toResponse(post);
    }

    @Transactional
    public PostingResponseDTO deletePosting(Long memberId, Long postingId) {
        requireRole(memberId, MemberRole.ADMIN);

        Post post = postingRepository.findByIdForUpdate(postingId).orElseThrow(PostingService::postingNotFound);
        PostingResponseDTO response = toResponse(post);
        postApplicationRepository.deleteByPostId(postingId);
        postingRepository.delete(post);
        return response;
    }

    @Transactional
    public PostingResponseDTO applyPosting(Long memberId, Long postingId) {
        Member member = requireRole(memberId, MemberRole.USER);

        Post post = postingRepository.findByIdForUpdate(postingId).orElseThrow(PostingService::postingNotFound);
        if (postApplicationRepository.existsByPostIdAndMemberId(postingId, memberId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 신청한 공고입니다.");
        }
        if (!isRecruiting(post)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "모집 기간이 아닙니다.");
        }
        if (isFull(post)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "모집 인원이 마감되었습니다.");
        }

        postApplicationRepository.save(PostApplication.builder().post(post).member(member).build());
        return toResponse(post);
    }

    @Transactional
    public PostingResponseDTO withdrawPosting(Long memberId, Long postingId) {
        requireRole(memberId, MemberRole.USER);

        Post post = postingRepository.findById(postingId).orElseThrow(PostingService::postingNotFound);
        PostApplication application = postApplicationRepository.findByPostIdAndMemberId(postingId, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신청 내역이 없습니다."));
        postApplicationRepository.delete(application);
        return toResponse(post);
    }

    private Member requireRole(Long memberId, MemberRole role) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "회원 정보를 찾을 수 없습니다. 다시 로그인해 주세요."));
        if (member.getRole() != role) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "권한이 없습니다.");
        }
        return member;
    }

    private boolean isRecruiting(Post post) {
        LocalDate today = LocalDate.now();
        boolean started = post.getRecruitmentStartDate() == null || !today.isBefore(post.getRecruitmentStartDate());
        boolean notEnded = post.getRecruitmentEndDate() == null || !today.isAfter(post.getRecruitmentEndDate());
        return started && notEnded;
    }

    /** 1365 값이 없거나 0인 공고는 모집인원 제한이 없는 것으로 본다. */
    private boolean isFull(Post post) {
        Integer capacity = post.getRecruitmentCapacity();
        return capacity != null && capacity > 0 && postApplicationRepository.countByPostId(post.getId()) >= capacity;
    }

    private PostingResponseDTO toResponse(Post post) {
        return PostingResponseDTO.from(post, postApplicationRepository.countByPostId(post.getId()));
    }

    private static ResponseStatusException postingNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "공고를 찾을 수 없습니다.");
    }
}
