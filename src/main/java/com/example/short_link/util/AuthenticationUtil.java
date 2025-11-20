package com.example.short_link.util;

import com.example.short_link.entity.User;
import com.example.short_link.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthenticationUtil {
    private final UserRepository userRepository;

    public User getCurrentAuthenticatedUser() {

        // Lấy đối tượng Authentication từ ngữ cảnh bảo mật hiện tại
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            // Không có thông tin xác thực hoặc là người dùng ẩn danh
            return null;
        }

        // Lấy đối tượng Principal (thường là UserDetails hoặc một String)
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();

            // Tải User Entity đầy đủ từ database
            Optional<User> userOptional = userRepository.findByEmail(username);

            // Trả về User Entity nếu tìm thấy, ngược lại là null
            return userOptional.orElse(null);
        }

        // Trả về null nếu Principal không phải là UserDetails (ví dụ: String 'anonymousUser')
        return null;
    }
}
