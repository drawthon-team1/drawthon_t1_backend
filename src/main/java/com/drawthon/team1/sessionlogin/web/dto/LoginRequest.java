package com.drawthon.team1.sessionlogin.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Setter
@Getter
public class LoginRequest {

    @Schema(example = "test0@abc.com")
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Size(max = 120, message = "이메일은 120자 이하여야 합니다.")
    private String email;

    @Schema(example = "password1234")
    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(max = 72, message = "비밀번호는 72자 이하여야 합니다.")
    private String password;


}
