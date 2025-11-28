package com.example.short_link.service.impl;

import com.example.short_link.dto.request.ChangePasswordRequest;
import com.example.short_link.dto.request.LoginRequest;
import com.example.short_link.dto.request.RegisterRequest;
import com.example.short_link.dto.request.UserSearchRequest;
import com.example.short_link.dto.response.AuthResponse;
import com.example.short_link.entity.User;
import com.example.short_link.enums.AuthProvider;
import com.example.short_link.enums.Role;
import com.example.short_link.exception.DataNotFoundException;
import com.example.short_link.exception.InvalidOtpException;
import com.example.short_link.exception.PermissionDenyException;
import com.example.short_link.exception.TooManyRequestsException;
import com.example.short_link.repository.UserRepository;
import com.example.short_link.repository.spec.UserSpecification;
import com.example.short_link.sercurity.jwt.JwtService;
import com.example.short_link.sercurity.user.CustomUserDetailsService;
import com.example.short_link.service.TokenService;
import com.example.short_link.service.UserService;
import com.example.short_link.util.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final RedisService redisService;
    private final UserAgentParsingUtil userAgentUtil;
    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final CookiesUtil cookiesUtil;
    private final AuthenticationUtil authenticationUtil;

    @Override
    public User register(RegisterRequest request) {
        Optional<User> optinalUser = userRepository.findByEmail(request.getEmail());

        //tìm xem user co ton tai khong
        if (optinalUser.isPresent()) {
            // tồn tại thì xem có phải ADMIN không
            if (optinalUser.get().getRole().equals(Role.ADMIN)) {
                throw new PermissionDenyException("Cannot create admin account");
            }

            throw new DataNotFoundException("User already Exists");
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
    public AuthResponse login(LoginRequest request,
                              HttpServletResponse response,
                              HttpServletRequest httpRequest) {
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());

        if (userOptional.isEmpty()) {
            throw new DataNotFoundException("User Not Found");
        }

        User existingUser = userOptional.get();

        if (existingUser.getProvider() == AuthProvider.GOOGLE) {
            throw new BadCredentialsException("This account was registered via Google. " +
                    "Use Google Login instead.");
        }

        if (!passwordEncoder.matches(request.getPassword(), existingUser.getPassword())) {
            throw new BadCredentialsException("Wrong password");
        }

        if (!existingUser.isActive()) {
            throw new BadCredentialsException("User is locked");
        }


        UserDetails userDetails = customUserDetailsService.loadUserByUsername(existingUser.getEmail());

        //TẠO ACCESS TOKEN
        String accessToken = jwtService.generateAccessToken(userDetails);

        // 6. Tạo refresh token mới
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        Instant expiresAt = jwtService.extractExpiration(refreshToken);


        // 7. Lấy device + IP
        String userAgentHeader = httpRequest.getHeader("User-Agent");
        String deviceName = userAgentUtil.getDevice(userAgentHeader)
                + " • " + userAgentUtil.getBrowser(userAgentHeader);
        String ipAddress = httpRequest.getRemoteAddr();

        //giới hạn token mỗi device
        tokenService.limitTokensPerDevice(existingUser, deviceName, 2);

        // 8. Lưu refresh token kèm device info
        tokenService.saveUserToken(existingUser, refreshToken, expiresAt, deviceName, ipAddress);

        cookiesUtil.setCookie(response, "access_token",
                accessToken, 15 * 60);   // 15 phút
        cookiesUtil.setCookie(response, "refresh_token",
                refreshToken, 7 * 24 * 60 * 60); // 7 ngày

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .fullName(existingUser.getFullName())
                .role(existingUser.getRole().toString())
                .pictureUrl(existingUser.getPictureUrl())
                .build();
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new DataNotFoundException("Invalid user"));
    }


    @Override
    public Page<User> searchUsers(UserSearchRequest request, Pageable pageable) {
        Specification<User> spec = Specification.unrestricted();

        if (request.getEmail() != null) {
            spec = spec.and(UserSpecification.containsEmail(request.getEmail()));
        }

        if (request.getFullName() != null) {
            spec = spec.and(UserSpecification.containsFullName(request.getFullName()));
        }

        if (request.getProvider() != null) {
            spec = spec.and(UserSpecification.hasProvider(request.getProvider()));
        }

        if (request.getIsActive() != null) {
            spec = spec.and(UserSpecification.isActive(request.getIsActive()));
        }

        return userRepository.findAll(spec, pageable);
    }

    @Override
    @Transactional
    public void logout(String accessToken, String refreshToken, HttpServletResponse response) {
        // 1. Blacklist Access Token (nếu có)
        if (StringUtils.hasText(accessToken)) {
            long remainingSeconds = jwtService.getRemainingSeconds(accessToken);
            if (remainingSeconds > 0) {
                redisService.blacklistToken(accessToken, remainingSeconds);
            }
        }

        // 2. Blacklist + Xóa Refresh Token (nếu có)
        if (StringUtils.hasText(refreshToken)) {
            // revoke
            tokenService.revokeRefreshToken(refreshToken);

            // Blacklist trong Redis (nếu hệ thống có nhiều instance)
            long refreshRemaining = jwtService.getRemainingSeconds(refreshToken);
            if (refreshRemaining > 0) {
                redisService.blacklistToken(refreshToken, refreshRemaining);
            }

        }

        // xóa trong cookie
        cookiesUtil.revokeCookies(response, "access_token", "refresh_token");

        // Optional: Clear SecurityContext (dù stateless nhưng vẫn nên)
        SecurityContextHolder.clearContext();
    }

    @Transactional
    @Override
    public void changePassword(ChangePasswordRequest request) {
        //Lấy thông tin người dùng hiện tại từ Security Context
        User currentUser = authenticationUtil.getCurrentAuthenticatedUser();

        User user = userRepository.findByEmail(currentUser.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found in security context"));

        // Xác thực Mật khẩu Cũ
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadCredentialsException("Old password not match");
        }

        //Xác thực Mật khẩu Mới
        if (!request.getNewPassword().equals(request.getConfirmationPassword())) {
            throw new BadCredentialsException("Passwords not match");
        }

        //Mã hóa và Lưu Mật khẩu Mới
        String newHashedPassword = passwordEncoder.encode(request.getNewPassword());
        user.setPassword(newHashedPassword);
        userRepository.save(user);

        // Thu hồi TẤT CẢ các Refresh Token cũ của người dùng.
        tokenService.revokeAllUserRefreshTokens(user);

        // Xóa khỏi SecurityContextHolder
        SecurityContextHolder.clearContext();
    }


    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        // Sinh số ngẫu nhiên từ 100000 đến 999999 (đảm bảo 6 chữ số)
        int otpInt = random.nextInt(900000) + 100000;
        return String.valueOf(otpInt);
    }

    @Override
    public void generateAndSendOtp(String email) {
        String normalizedEmail = email.trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy email này"));

        // Rate limit: tối đa 5 lần/giờ
        String rateKey = "otp:rate:" + normalizedEmail;
        long count = redisService.incrementAndExpire(rateKey, 1, Duration.ofHours(1));
        if (count > 5) {
            throw new TooManyRequestsException("Quá nhiều yêu cầu OTP. Vui lòng thử lại sau 1 giờ");
        }

        // Cooldown: không cho gửi liên tục trong 60 giây
        String cooldownKey = "otp:cooldown:" + normalizedEmail;
        if (redisService.exists(cooldownKey)) {
            long ttl = redisService.getTtl(cooldownKey);
            throw new TooManyRequestsException(
                    String.format("Vui lòng đợi %d giây trước khi yêu cầu OTP mới", ttl)
            );
        }

        String otp = generateOtp();
        Instant expiresAt = Instant.now().plus(5, ChronoUnit.MINUTES);

        // Lưu OTP vào Redis (tự động hết hạn sau 5 phút)
        redisService.saveOtp(normalizedEmail, otp, Duration.ofMinutes(5));

        // Đặt cooldown 60 giây
        redisService.set(cooldownKey, "1", Duration.ofSeconds(60));

        // Gửi email bất đồng bộ (không block request)
        emailService.sendOtpEmail(normalizedEmail, otp, expiresAt);

    }

    @Override
    public String verifyOtpAndGenerateResetToken(String email, String otp) {
        String normalizedEmail = email.trim().toLowerCase();

        // Lấy và xóa OTP (one-time use)
        String storedOtp = redisService.getOtpAndRemove(normalizedEmail);

        if (storedOtp == null || !storedOtp.equals(otp)) {
            throw new InvalidOtpException("Mã OTP không đúng hoặc đã hết hạn");
        }

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new DataNotFoundException("User not found"));

        // Tạo token reset password (15 phút, chỉ dùng 1 lần)
        String resetToken = jwtService.generatePasswordResetToken(normalizedEmail);

        return resetToken; // frontend sẽ dùng token này để đổi mật khẩu
    }

    @Override
    public void resetPasswordByToken(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new DataNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // ĐÁ HẾT THIẾT BỊ RA
        tokenService.revokeAllUserTokens(user);
    }
}
