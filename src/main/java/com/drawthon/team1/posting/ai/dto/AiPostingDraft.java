package com.drawthon.team1.posting.ai.dto;

import com.drawthon.team1.posting.entity.RewardType;
import com.drawthon.team1.posting.entity.WorkType;

import java.util.List;
import java.util.Optional;

/** OpenAI Structured Outputs 전용 모델. 날짜와 시간은 파싱 전 문자열로 받는다. */
public class AiPostingDraft {
    public Optional<String> programTitle;
    public Optional<String> programContent;
    public Optional<String> programStartDate;
    public Optional<String> programEndDate;
    public Optional<String> activityStartTime;
    public Optional<String> activityEndTime;
    public Optional<String> recruitmentStartDate;
    public Optional<String> recruitmentEndDate;
    public Optional<Integer> recruitmentCapacity;
    public Optional<String> activityPlace;
    public Optional<RewardType> rewardType;
    public Optional<Integer> reward;
    public Optional<String> address;
    public Optional<WorkType> workType;
    public List<String> warnings;
}
