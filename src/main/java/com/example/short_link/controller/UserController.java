package com.example.short_link.controller;

import com.example.short_link.dto.request.RegisterRequest;
import com.example.short_link.dto.request.UserSearchRequest;
import com.example.short_link.dto.response.RegisterResponse;
import com.example.short_link.dto.response.UserResponse;
import com.example.short_link.entity.User;
import com.example.short_link.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    // lấy tất cả user theo filter
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @ModelAttribute UserSearchRequest searchRequest,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
            ){

        Page<User> result = userService.searchUsers(searchRequest, pageable);

//        Page<UserResponse> responses = result.map(
//                user -> UserResponse.fromEntity(user)
//        );

        // dùng lamda
        Page<UserResponse> responses = result.map(UserResponse::fromEntity);

        return ResponseEntity.ok(responses);
    }


}
