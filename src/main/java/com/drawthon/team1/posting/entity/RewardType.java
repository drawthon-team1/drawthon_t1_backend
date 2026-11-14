package com.drawthon.team1.posting.entity;

/**
 * 공고 리워드 종류. 컬럼으로 저장하지 않고 공고 출처로 정한다.
 * VOLUNTEER_HOURS : 1365에서 가져온 공고 (reward = 봉사시간, 시간)
 * WAGE : 플랫폼에서 직접 작성한 공고 (reward = 임금, 원)
 */
public enum RewardType {
    VOLUNTEER_HOURS,
    WAGE
}
