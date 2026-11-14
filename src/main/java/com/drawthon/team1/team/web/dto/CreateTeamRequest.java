package com.drawthon.team1.team.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTeamRequest {

    @Schema(example = "드로우톤 1팀")
    @NotBlank(message = "팀 이름은 필수입니다.")
    @Size(max = 50, message = "팀 이름은 50자 이하여야 합니다.")
    private String name;
}
