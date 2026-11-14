package com.drawthon.team1.posting.entity;

import com.drawthon.team1.posting.dto.PostingCreateDTO;
import com.drawthon.team1.posting.dto.PostingUpdateDTO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/**
 * 1365 api를 통해 읽어오는 데이터에 맞춰서 컬럼을 작성
 * 1365 응답 항목명과 필드의 매핑은 VolunteerApiClient.toPost에서 관리한다.
 * 우리 서비스에서 직접 작성한 공고는 1365 전용 항목(프로그램등록번호 등)이 비어 있다.
 */

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 프로그램등록번호 (1365에서 가져온 공고만 값이 있음) */
    @Column(unique = true)
    private String programRegistrationNumber;

    /** 봉사제목 */
    @Column(length = 500)
    private String programTitle;

    /** 내용 */
    @Lob
    private String programContent;

    /** 모집상태 (가져온 시점의 1365 값) */
    private String recruitmentStatus;

    /** 봉사시작일자 */
    private LocalDate programStartDate;

    /** 봉사종료일자 */
    private LocalDate programEndDate;

    /** 봉사시작시간 */
    private String activityStartTime;

    /** 봉사종료시간 */
    private String activityEndTime;

    /** 모집시작일 */
    private LocalDate recruitmentStartDate;

    /** 모집종료일 */
    private LocalDate recruitmentEndDate;

    /** 모집인원 */
    private Integer recruitmentCapacity;

    /** 신청인원수 (가져온 시점의 1365 값, 이 서비스의 신청 인원과는 별개) */
    private Integer externalApplicantCount;

    /** 활동요일 */
    private String activityWeekdays;

    /** 봉사분야 */
    private String serviceCategoryCode;

    /** 성인가능여부 */
    private String adultParticipationAllowed;

    /** 청소년가능여부 */
    private String youthParticipationAllowed;

    /** 단체가능여부 */
    private String groupParticipationAllowed;

    /** 공무원가능여부 */
    private String publicOfficialParticipationAllowed;

    /** 가족가능여부 */
    private String familyParticipationAllowed;

    /** 모집기관(주관기관명) */
    private String hostOrganizationName;

    /** 등록기관(나눔주체명) */
    private String registeringOrganizationName;

    /** 봉사장소 */
    @Column(length = 500)
    private String activityPlace;

    /** 담당자명 */
    private String managerName;

    /** 전화번호 */
    private String managerPhoneNumber;

    /** FAX번호 */
    private String managerFaxNumber;

    /** 담당자 주소 */
    @Column(length = 500)
    private String managerAddress;

    /** 이메일 */
    private String managerEmail;

    /** 시도코드 */
    private String provinceCode;

    /** 시군구코드 */
    private String districtCode;

    /** 봉사장소주소1 */
    @Column(length = 500)
    private String activityAddress1;

    /** 봉사장소주소2 */
    @Column(length = 500)
    private String activityAddress2;

    /** 봉사장소주소3 */
    @Column(length = 500)
    private String activityAddress3;

    /** 봉사장소1위도경도 */
    private String activityCoordinates1;

    /** 봉사장소2위도경도 */
    private String activityCoordinates2;

    /** 봉사장소3위도경도 */
    private String activityCoordinates3;

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
    }
}
