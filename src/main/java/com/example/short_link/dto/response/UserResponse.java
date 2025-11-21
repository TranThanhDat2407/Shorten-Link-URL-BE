package com.example.short_link.dto.response;

import com.example.short_link.entity.User;
import com.example.short_link.enums.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String fullName;
    private String pictureUrl;
    private Boolean isActive;
    private Role role;

    public static UserResponse fromEntity(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .pictureUrl(user.getPictureUrl())
                .isActive(user.isActive())
                .role(user.getRole())
                .build();
    }
}
