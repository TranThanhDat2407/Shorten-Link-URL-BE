package com.example.short_link.dto.request;

import com.example.short_link.enums.AuthProvider;
import lombok.Data;

@Data
public class UserSearchRequest {
    String email;
    String fullName;
    Boolean isActive;
    AuthProvider provider;
    Long totalLink;
}
