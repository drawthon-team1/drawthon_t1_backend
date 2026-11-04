package com.drawthon.team1.posting.service;

import com.drawthon.team1.posting.client.VolunteerApiClient;
import com.drawthon.team1.posting.dto.PostingCreateDTO;
import com.drawthon.team1.posting.dto.PostingExternalDTO;
import com.drawthon.team1.posting.dto.PostingInfoDTO;
import com.drawthon.team1.posting.dto.PostingResponseDTO;
import com.drawthon.team1.posting.dto.PostingUpdateDTO;
import com.drawthon.team1.posting.entity.Post;
import com.drawthon.team1.posting.entity.PostApplication;
import com.drawthon.team1.posting.repository.PostApplicationRepository;
import com.drawthon.team1.posting.repository.PostingRepository;
import com.drawthon.team1.user.entity.Role;
import com.drawthon.team1.user.entity.User;
import com.drawthon.team1.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

/**
 * 공고 읽어오기 -> 공고 목록 대시보드 반환 : 공통
 * 공고 등록 / 수정 / 삭제 : 관리자(ADMIN)
 * 공고 신청 / 철회 : 사용자(USER)
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
    private final UserRepository userRepository;
    private final VolunteerApiClient volunteerApiClient;

    @Transactional(readOnly = true)
    public List<PostingInfoDTO> getPostings() {
        return postingRepository.findAllByOrderByIdDesc().stream()
                .map(PostingInfoDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PostingInfoDTO getPosting(Long postingId) {
        Post post = postingRepository.findById(postingId).orElseThrow(PostingService::postingNotFound);
        return PostingInfoDTO.detailFrom(post, postApplicationRepository.countByPostId(postingId));
    }

    public List<PostingExternalDTO> searchExternalPostings(String keyword, int pageNo, int numOfRows) {
        return volunteerApiClient.search(keyword, pageNo, numOfRows);
    }

    /**
     * 외부 api 호출 동안 DB 커넥션을 점유하지 않도록 트랜잭션을 걸지 않는다.
     */
    public PostingResponseDTO createPosting(Long userId, PostingCreateDTO postingCreateDTO) {
        requireRole(userId, Role.ADMIN);

        String programRegistrationNumber = postingCreateDTO.getProgramRegistrationNumber();
        if (postingRepository.existsByProgramRegistrationNumber(programRegistrationNumber)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 등록된 공고입니다.");
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
    public PostingResponseDTO updatePosting(Long userId, Long postingId, PostingUpdateDTO postingUpdateDTO) {
        requireRole(userId, Role.ADMIN);

        Post post = postingRepository.findById(postingId).orElseThrow(PostingService::postingNotFound);
        post.update(postingUpdateDTO);
        return toResponse(post);
    }

    @Transactional
    public PostingResponseDTO deletePosting(Long userId, Long postingId) {
        requireRole(userId, Role.ADMIN);

        Post post = postingRepository.findByIdForUpdate(postingId).orElseThrow(PostingService::postingNotFound);
        PostingResponseDTO response = toResponse(post);
        postApplicationRepository.deleteByPostId(postingId);
        postingRepository.delete(post);
        return response;
    }

    @Transactional
    public PostingResponseDTO applyPosting(Long userId, Long postingId) {
        User user = requireRole(userId, Role.USER);

        Post post = postingRepository.findByIdForUpdate(postingId).orElseThrow(PostingService::postingNotFound);
        if (postApplicationRepository.existsByPostIdAndUserId(postingId, userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 신청한 공고입니다.");
        }
        if (!isRecruiting(post)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "모집 기간이 아닙니다.");
        }
        if (isFull(post)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "모집 인원이 마감되었습니다.");
        }

        postApplicationRepository.save(PostApplication.builder().post(post).user(user).build());
        return toResponse(post);
    }

    @Transactional
    public PostingResponseDTO withdrawPosting(Long userId, Long postingId) {
        requireRole(userId, Role.USER);

        Post post = postingRepository.findById(postingId).orElseThrow(PostingService::postingNotFound);
        PostApplication application = postApplicationRepository.findByPostIdAndUserId(postingId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신청 내역이 없습니다."));
        postApplicationRepository.delete(application);
        return toResponse(post);
    }

    private User requireRole(Long userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        if (user.getRole() != role) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "권한이 없습니다.");
        }
        return user;
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
