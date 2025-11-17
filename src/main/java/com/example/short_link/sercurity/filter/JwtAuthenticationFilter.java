package com.example.short_link.sercurity.filter;

import com.example.short_link.sercurity.jwt.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@AllArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        //Trường hợp chưa đăng nhập nên chưa có token:
        // header không có token bỏ qua;
        if(authHeader == null || !authHeader.startsWith("Bearer ")){
            filterChain.doFilter(request, response);
            return;
        }

        // Trường hợp sau khi đăng nhập và User đã có token
        // lấy token
        jwt = authHeader.substring(7); // bỏ Bearer + dấu cách 7 kí tự
        username = jwtService.extractEmail(jwt); // lấy subject trong jwt để lấy email

        //tìm thấy user và lưu tất cả principal + authorities vào trong SercurityContextHolder
        if(username != null && SecurityContextHolder.getContext().getAuthentication() == null){
            UserDetails user = userDetailsService.loadUserByUsername(username);

            if (jwtService.isValidateToken(jwt, user)) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

                // lưu những thứ như IP của người Request
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                //lưu principal + authorities
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
