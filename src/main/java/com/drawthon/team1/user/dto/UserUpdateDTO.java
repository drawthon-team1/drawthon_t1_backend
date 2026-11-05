package com.drawthon.team1.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 수정 dto
 * PUT 요청이므로 수정 가능한 항목 전체를 보내야 한다. role은 수정할 수 없다.
 */

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateDTO {

    @NotBlank
    @Size(max = 50)
    private String name;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;
}
