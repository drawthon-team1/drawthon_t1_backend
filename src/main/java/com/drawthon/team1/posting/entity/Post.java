package com.drawthon.team1.posting.entity;

import com.drawthon.team1.posting.dto.PostingCreateDTO;
import com.drawthon.team1.posting.dto.PostingUpdateDTO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * 공고. 1365에서 가져온 공고와 관리자가 직접 작성한 공고를 함께 저장한다.
 * MVP 기능(목록·상세 조회, 생성·수정·삭제, 신청·철회)에 필요한 항목만 둔다.
 * 1365 응답 항목명과 필드의 매핑은 VolunteerApiClient.toPost에서 관리한다.
 *
 * 리워드는 reward 컬럼 하나로 표현한다. 종류(getRewardType)는 저장하지 않고 출처로 정한다.
 * - 1365에서 가져온 공고: 봉사시간(시간) = 활동 종료시간 - 시작시간
 * - 직접 작성한 공고: 임금(원) = 관리자가 입력
 */

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Post {
    /** 모집 종료일까지 이 일수 이하로 남으면 마감 직전으로 본다. */
    private static final int CLOSING_SOON_DAYS = 3;
    /** 1365가 주소 대신 넣는 안내 문구. 주소로 저장하지 않는다. */
    private static final String ADDRESS_PLACEHOLDER = "수요처 지도";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 1365 프로그램등록번호 (1365에서 가져온 공고만 값이 있음, 중복 등록 방지용) */
    @Column(unique = true)
    private String programRegistrationNumber;

    /** 봉사제목 */
    @Column(length = 500)
    private String programTitle;

    /** 내용 */
    @Lob
    private String programContent;

    /** 봉사시작일자 */
    private LocalDate programStartDate;

    /** 봉사종료일자 */
    private LocalDate programEndDate;

    /** 봉사시작시간 */
    private String activityStartTime;

    /** 봉사종료시간 */
    private String activityEndTime;

    /** 모집시작일 (신청 가능 여부 판단) */
    private LocalDate recruitmentStartDate;

    /** 모집종료일 (신청 가능 여부 판단) */
    private LocalDate recruitmentEndDate;

    /** 모집인원 (없거나 0이면 제한 없음) */
    private Integer recruitmentCapacity;

    /** 등록기관 (1365 나눔주체명) */
    private String registeringOrganizationName;

    /** 봉사장소 */
    @Column(length = 500)
    private String activityPlace;

    /** 리워드 값. 1365 공고는 봉사시간(시간), 직접 작성 공고는 임금(원) */
    private Integer reward;

    /** 활동 주소 (지역 필터용). 1365 공고는 1365 주소, 직접 작성 공고는 관리자가 입력 */
    @Column(length = 500)
    private String address;

    /** 작업 종류. 1365 공고는 제목·내용 키워드로 추정한 값 */
    @Enumerated(EnumType.STRING)
    private WorkType workType;

    /** 모집 기간·모집 인원·신청 인원으로 계산한 현재 모집 상태 */
    public RecruitmentStatus recruitmentStatus(LocalDate today, long applicantCount) {
        if (recruitmentStartDate != null && today.isBefore(recruitmentStartDate)) {
            return RecruitmentStatus.UPCOMING;
        }
        boolean ended = recruitmentEndDate != null && today.isAfter(recruitmentEndDate);
        boolean full = recruitmentCapacity != null && recruitmentCapacity > 0 && applicantCount >= recruitmentCapacity;
        if (ended || full) {
            return RecruitmentStatus.CLOSED;
        }
        if (recruitmentEndDate != null && !today.plusDays(CLOSING_SOON_DAYS).isBefore(recruitmentEndDate)) {
            return RecruitmentStatus.CLOSING_SOON;
        }
        return RecruitmentStatus.RECRUITING;
    }

    /**
     * 주소에 해당 지역 이름이 있는지 확인한다. 부분 문자열이 아니라 단어 단위로 비교해
     * "동구"가 "남동구"에 걸리지 않게 한다. "포항시 남구"처럼 여러 단어면 모두 있어야 한다.
     */
    public boolean isLocatedIn(String region) {
        if (address == null || region == null || region.isBlank()) {
            return false;
        }
        List<String> addressWords = Arrays.asList(address.split("[\\s(),]+"));
        return Arrays.stream(region.trim().split("\\s+")).allMatch(addressWords::contains);
    }

    /** 1365 주소 값을 저장할 주소로 바꾼다. 비어 있거나 안내 문구면 null. */
    public static String normalizeAddress(String address) {
        if (address == null || address.isBlank() || ADDRESS_PLACEHOLDER.equals(address.trim())) {
            return null;
        }
        return address.trim();
    }

    public Integer activityStartMinutes() {
        return toMinutes(activityStartTime);
    }

    public Integer activityEndMinutes() {
        return toMinutes(activityEndTime);
    }

    /** 1365 프로그램등록번호가 있으면 1365에서 가져온 봉사시간 공고, 없으면 직접 작성한 임금 공고 */
    public RewardType getRewardType() {
        return programRegistrationNumber == null ? RewardType.WAGE : RewardType.VOLUNTEER_HOURS;
    }

    /**
     * 활동 시작·종료 시간으로 봉사시간(시간)을 계산한다.
     * 1365는 "9", "18"처럼 시 단위로 주고, 수정 화면에서 "09:30" 형식이 와도 계산한다. 1시간 미만은 버린다.
     * 값이 없거나 형식이 다르거나 종료가 시작보다 이르면 null.
     */
    public static Integer volunteerHours(String startTime, String endTime) {
        Integer start = toMinutes(startTime);
        Integer end = toMinutes(endTime);
        if (start == null || end == null || end <= start) {
            return null;
        }
        int hours = (end - start) / 60;
        return hours > 0 ? hours : null;
    }

    private static Integer toMinutes(String time) {
        if (time == null || time.isBlank()) {
            return null;
        }
        String[] parts = time.trim().split(":");
        try {
            int hour = Integer.parseInt(parts[0]);
            int minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return hour * 60 + minute;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Post from(PostingCreateDTO dto) {
        return Post.builder()
                .programTitle(dto.getProgramTitle())
                .programContent(dto.getProgramContent())
                .programStartDate(dto.getProgramStartDate())
                .programEndDate(dto.getProgramEndDate())
                .activityStartTime(dto.getActivityStartTime())
                .activityEndTime(dto.getActivityEndTime())
                .recruitmentStartDate(dto.getRecruitmentStartDate())
                .recruitmentEndDate(dto.getRecruitmentEndDate())
                .recruitmentCapacity(dto.getRecruitmentCapacity())
                .activityPlace(dto.getActivityPlace())
                .reward(dto.getReward())
                .address(normalizeAddress(dto.getAddress()))
                .workType(dto.getWorkType() == null ? WorkType.ETC : dto.getWorkType())
                .build();
    }

    public void update(PostingUpdateDTO dto) {
        this.programTitle = dto.getProgramTitle();
        this.programContent = dto.getProgramContent();
        this.programStartDate = dto.getProgramStartDate();
        this.programEndDate = dto.getProgramEndDate();
        this.activityStartTime = dto.getActivityStartTime();
        this.activityEndTime = dto.getActivityEndTime();
        this.recruitmentStartDate = dto.getRecruitmentStartDate();
        this.recruitmentEndDate = dto.getRecruitmentEndDate();
        this.recruitmentCapacity = dto.getRecruitmentCapacity();
        this.activityPlace = dto.getActivityPlace();
        if (getRewardType() == RewardType.VOLUNTEER_HOURS) {
            this.reward = volunteerHours(activityStartTime, activityEndTime);
        } else if (dto.getReward() != null) {
            this.reward = dto.getReward();
        }
        if (dto.getAddress() != null) {
            this.address = normalizeAddress(dto.getAddress());
        }
        if (dto.getWorkType() != null) {
            this.workType = dto.getWorkType();
        }
    }
}
