package com.example.short_link.sercurity.config;

import com.example.short_link.sercurity.filter.JwtAuthenticationFilter;
import com.example.short_link.sercurity.oauth.CustomOAuth2SuccessHandler;
import com.example.short_link.sercurity.user.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
@EnableMethodSecurity()
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    private final CustomOAuth2SuccessHandler successHandler;

    @Value("${api.prefix}")
    private String apiPrefix;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(request -> request
                                .requestMatchers(
                                   String.format("%s/auth/**", apiPrefix)


                                ).permitAll()
                                .requestMatchers(
                                        "/login/oauth2/**",
                                        "/oauth2/**",
                                        "/login/**"
                                ).permitAll()
                                .requestMatchers(HttpMethod.POST
                                        ,String.format("%s/short-link/**", apiPrefix)).permitAll()
                                .requestMatchers(HttpMethod.GET
                                        ,String.format("%s/short-link/**", apiPrefix)).permitAll()
                                .requestMatchers("/error").permitAll()
                                .anyRequest().authenticated()
                        )
                //chặn api không có token sang oauth2
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((
                                request,
                                response,
                                authException) -> {
                            response.sendError(
                                    HttpServletResponse.SC_UNAUTHORIZED,
                                    "Unauthorized: Authentication required."
                            );
                        })
                )
                .oauth2Login(oauth -> oauth
                        .successHandler(successHandler)
                )

//                .userDetailsService(userDetailsService)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);



                return http.build();
    }

    @Bean
    public PasswordEncoder PasswordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }



}
