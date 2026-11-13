package com.drawthon.team1.sessionlogin.web.dto;

import com.drawthon.team1.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Setter
@Getter
public class SignupRequest {

    @Schema(example = "test0@abc.com")
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Size(max = 120, message = "이메일은 120자 이하여야 합니다.")
    private String email;

    @Schema(example = "password1234")
    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 3, max = 72, message = "비밀번호는 8~72자여야 합니다.")
    private String password;

    @Schema(example = "test")
    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(max = 40, message = "닉네임은 40자 이하여야 합니다.")
    private String nickname;

    /** MVP 데모용: 프론트가 보낸 role로 가입한다. 보내지 않으면 USER. */
    @Schema(example = "USER", description = "ADMIN 또는 USER. 생략하면 USER")
    private Role role;

}
