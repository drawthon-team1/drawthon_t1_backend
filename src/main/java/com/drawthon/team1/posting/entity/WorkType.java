package com.drawthon.team1.posting.entity;

import java.util.List;

/**
 * 작업 종류.
 * 직접 작성한 공고는 관리자가 고르고, 1365 공고는 1365에 해당 항목이 없어 제목·내용의 키워드로 추정한다.
 */
public enum WorkType {
    HARVEST("수확", "따기", "캐기"),
    SORTING_PACKING("선별", "포장"),
    PLANTING_CULTIVATION("파종", "재배", "모종", "심기"),
    WEEDING("제초", "잡초", "풀뽑기", "김매기"),
    ETC;

    private final List<String> keywords;

    WorkType(String... keywords) {
        this.keywords = List.of(keywords);
    }

    /** 키워드가 처음 맞는 종류를 반환하고, 맞는 것이 없으면 ETC. */
    public static WorkType infer(String... texts) {
        for (WorkType type : values()) {
            for (String text : texts) {
                if (text != null && type.keywords.stream().anyMatch(text::contains)) {
                    return type;
                }
            }
        }
        return ETC;
    }
}
