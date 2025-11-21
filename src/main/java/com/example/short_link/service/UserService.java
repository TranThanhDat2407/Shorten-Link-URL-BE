package com.example.short_link.service;

import com.example.short_link.dto.request.LoginRequest;
import com.example.short_link.dto.request.RegisterRequest;
import com.example.short_link.dto.request.UserSearchRequest;
import com.example.short_link.dto.response.AuthResponse;
import com.example.short_link.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    User register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    User findByEmail(String email);

    Page<User> searchUsers(UserSearchRequest request, Pageable pageable);
}
