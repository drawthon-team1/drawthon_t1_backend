package com.drawthon.team1.user.dto;

import com.drawthon.team1.user.entity.Role;
import com.drawthon.team1.user.entity.User;
import lombok.Builder;
import lombok.Getter;

/**
 * 사용자 공통 반환 dto
 */

@Getter
@Builder
public class UserResponseDTO {
    private final Long id;
    private final String name;
    private final String email;
    private final Role role;

    public static UserResponseDTO from(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
