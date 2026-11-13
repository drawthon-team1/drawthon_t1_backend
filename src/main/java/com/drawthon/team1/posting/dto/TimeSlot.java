package com.drawthon.team1.posting.dto;

/**
 * 공고 목록 필터: 활동 시간대. 활동 시간(시작~종료)이 시간대와 조금이라도 겹치면 해당한다.
 * 예: 9시~17시 공고는 MORNING과 AFTERNOON 모두에 해당한다.
 */
public enum TimeSlot {
    /** 새벽 00~06시 */
    DAWN(0, 6),
    /** 오전 06~12시 */
    MORNING(6, 12),
    /** 오후 12~18시 */
    AFTERNOON(12, 18),
    /** 저녁 18~22시 */
    EVENING(18, 22),
    /** 야간 22~24시 */
    NIGHT(22, 24);

    private final int startMinute;
    private final int endMinute;

    TimeSlot(int startHour, int endHour) {
        this.startMinute = startHour * 60;
        this.endMinute = endHour * 60;
    }

    /**
     * @param activityStart 활동 시작 시각(분). 없으면 해당하지 않는다
     * @param activityEnd 활동 종료 시각(분). 없거나 시작보다 이르면 시작 시각이 속한 시간대만 해당한다
     */
    public boolean matches(Integer activityStart, Integer activityEnd) {
        if (activityStart == null) {
            return false;
        }
        if (activityEnd == null || activityEnd <= activityStart) {
            return startMinute <= activityStart && activityStart < endMinute;
        }
        return activityStart < endMinute && activityEnd > startMinute;
    }
}
