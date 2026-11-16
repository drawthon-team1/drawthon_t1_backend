package com.drawthon.team1.posting.ai.dto;

import com.drawthon.team1.posting.entity.RewardType;
import com.drawthon.team1.posting.entity.WorkType;

import java.time.LocalDate;
import java.util.List;

public record PostingDraftResponse(
        String programTitle,
        String programContent,
        LocalDate programStartDate,
        LocalDate programEndDate,
        String activityStartTime,
        String activityEndTime,
        LocalDate recruitmentStartDate,
        LocalDate recruitmentEndDate,
        Integer recruitmentCapacity,
        String activityPlace,
        RewardType rewardType,
        Integer reward,
        String address,
        WorkType workType,
        List<String> missingFields,
        List<String> warnings
) {
}
