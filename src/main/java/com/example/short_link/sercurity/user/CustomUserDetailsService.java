package com.example.short_link.sercurity.user;

import com.example.short_link.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)// tìm user có email.

                // nếu có thì trả về 1 userDetail có các
                // thông tin principal, credential, authorities
                .map(CustomUserDetails::new)
                // .map(user -> new CustomUserDetails(user)) // cách ghi khác

                // không có thì ngoại lệ UsernameNotFound
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    }
}
