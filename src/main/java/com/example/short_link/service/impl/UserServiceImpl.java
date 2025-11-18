package com.example.short_link.service.impl;

import com.example.short_link.dto.request.LoginRequest;
import com.example.short_link.dto.request.RegisterRequest;
import com.example.short_link.dto.response.AuthResponse;
import com.example.short_link.entity.User;
import com.example.short_link.enums.AuthProvider;
import com.example.short_link.enums.Role;
import com.example.short_link.exception.DataNotFoundException;
import com.example.short_link.exception.PermissionDenyException;
import com.example.short_link.exception.UserNotFoundException;
import com.example.short_link.repository.UserRepository;
import com.example.short_link.sercurity.jwt.JwtService;
import com.example.short_link.sercurity.user.CustomUserDetailsService;
import com.example.short_link.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtService jwtService;

    @Override
    public User register(RegisterRequest request) {
        Optional<User> optinalUser = userRepository.findByEmail(request.getEmail());

        //tìm xem user co ton tai khong
        if(optinalUser.isPresent()){
            // tồn tại thì xem có phải ADMIN không
            if(optinalUser.get().getRole().equals(Role.ADMIN)){
                throw new PermissionDenyException("Cannot create admin account");
            }

            throw new UserNotFoundException("User already Exists");
        }



        // tìm index của @
        int indexAt = request.getEmail().indexOf('@');

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                //fullname = subtring trước @
                .fullName(request.getEmail().substring(0, indexAt))
                .role(Role.USER)
                .provider(AuthProvider.LOCAL)
                .isActive(true)
                .build();

        userRepository.save(user);
        return user;
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Optional<User> user = userRepository.findByEmail(request.getEmail());

        if(user.isEmpty()){
            throw new UserNotFoundException("User Not Found");
        }

        User existingUser = user.get();

        if (!passwordEncoder.matches(request.getPassword(), existingUser.getPassword())) {
            throw new BadCredentialsException
                    ("Wrong password");
        }

        if (!existingUser.isActive()) {
            throw new DataNotFoundException("User is locked");
        }

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(existingUser.getEmail());
        String token = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .build();
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Invalid user"));
    }
}
