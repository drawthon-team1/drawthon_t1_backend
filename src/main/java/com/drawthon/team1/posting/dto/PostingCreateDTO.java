package com.drawthon.team1.posting.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공고 모집 생성 dto
 * 1365의 프로그램등록번호를 받아 상세 정보를 가져와 공고로 등록한다.
 */

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostingCreateDTO {

    @NotBlank
    private String programRegistrationNumber;
}
