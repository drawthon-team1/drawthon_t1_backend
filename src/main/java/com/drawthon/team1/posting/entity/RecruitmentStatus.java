package com.drawthon.team1.posting.entity;

/**
 * 모집 상태. 저장하지 않고 모집 기간·모집 인원·신청 인원으로 조회 시점에 계산한다 (Post.recruitmentStatus).
 * UPCOMING : 모집 시작 전
 * RECRUITING : 모집 중
 * CLOSING_SOON : 마감 직전 (모집 중이면서 모집 종료일까지 3일 이하)
 * CLOSED : 모집 완료 (모집 종료일이 지났거나 모집 인원이 다 참)
 */
public enum RecruitmentStatus {
    UPCOMING,
    RECRUITING,
    CLOSING_SOON,
    CLOSED
}
