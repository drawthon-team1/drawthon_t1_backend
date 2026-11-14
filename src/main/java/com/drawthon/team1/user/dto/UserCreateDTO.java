package com.drawthon.team1.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 생성 dto
 * role은 요청으로 받지 않는다. 생성되는 사용자는 항상 USER이다.
 */

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateDTO {

    @NotBlank
    @Size(max = 50)
    private String name;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;
}
