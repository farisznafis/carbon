package com.kct.jcredit.core_backend.security;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.service.TokenBlacklistService;
import com.kct.jcredit.core_backend.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, TokenBlacklistService tokenBlacklistService) {
        this.jwtUtil = jwtUtil;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                // validate token
                String username = jwtUtil.extractUsername(token);
                long issuedAtMillis = jwtUtil.extractIssuedAtMillis(token);
                Long passwordChangedAtMillis = tokenBlacklistService.getPasswordChangedAt(username);
                if (passwordChangedAtMillis != null && issuedAtMillis < passwordChangedAtMillis) {
                    // Token was issued before the latest password change
                    SecurityContextHolder.clearContext();
                    sendUnauthorizedResponse(response, ResponseConstants.MSG_RELOGIN_REQUIRED);
                    return;
                }
                String role = jwtUtil.extractRole(token);
                List<GrantedAuthority> authorities = new ArrayList<>();
                if (role.equals("master")) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_user"));
                }
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));

                // set principal (you can store more info if needed)
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, token, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                // invalid token -> clear context
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
            "{\"statusCode\":\"" + ResponseConstants.UNAUTHORIZED + "\",\"message\":\"" + message + "\"}"
        );
    }
}
