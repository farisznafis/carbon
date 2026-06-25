package com.kct.jcredit.core_backend.security;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.constant.RoleConstants;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingUser;
import com.kct.jcredit.core_backend.enums.SubscriptionStatus;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
import com.kct.jcredit.core_backend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompanyAuthorizationFilter extends OncePerRequestFilter {

    private final ParticipatingCompanyRepository companyRepository;
    private final ParticipatingUserRepository userRepository;
    private final JwtUtil jwtUtil;

    private static final String MONITORING_API = "/api/monitoring";
    private static final String PROJECT_API = "/api/project";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authentication.getCredentials().toString();
        String companyId = jwtUtil.extractCompanyId(token);
        String role = jwtUtil.extractRole(token);

        if (companyId == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<ParticipatingCompany> companyOpt = companyRepository.findByCompanyId(companyId);
        
        if (companyOpt.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        ParticipatingCompany company = companyOpt.get();
        if (!company.getIsActive()) {
            log.warn("Company inactive or suspended, blocking request: {}", request.getRequestURI());
            sendErrorResponse(response, ResponseConstants.FORBIDDEN, ResponseConstants.MSG_FORBIDDEN);
            return;
        }

        if (company.getSubscriptionStatus() == SubscriptionStatus.SUSPENDED) {
            String requestUri = request.getRequestURI();
            boolean isMonitoringPost = request.getMethod().equals("POST") && requestUri.startsWith(MONITORING_API);
            boolean isProjectGet = request.getMethod().equals("GET") && requestUri.startsWith(PROJECT_API);

            if (isMonitoringPost || isProjectGet) {
                log.warn("Company inactive or suspended, blocking request: {}", request.getRequestURI());
                sendErrorResponse(response, ResponseConstants.SUSPENDED, ResponseConstants.MSG_SUSPENDED);
                return;
            }
        }

        if (RoleConstants.USER.equals(role)) {
            Optional<ParticipatingUser> userOpt = userRepository.findByUsernameAndCompany_Id(
                    jwtUtil.extractUsername(token), company.getId());
            if (userOpt.isPresent()) {
                ParticipatingUser user = userOpt.get();
                if (user.getSite() != null) {
                    request.setAttribute("assignedSiteId", user.getSite().getId());
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/auth/") || path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui");
    }

    private void sendErrorResponse(HttpServletResponse response, String statusCode, String message) throws IOException {
        response.setStatus(mapStatusCodeToHttpStatus(statusCode));
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
            "{\"statusCode\":\"" + statusCode + "\",\"message\":\"" + message + "\"}"
        );
    }

    private int mapStatusCodeToHttpStatus(String statusCode) {
        switch (statusCode) {
            case "NOT_FOUND":
                return HttpServletResponse.SC_NOT_FOUND;
            case "FORBIDDEN":
                return HttpServletResponse.SC_FORBIDDEN;
            case "UNAUTHORIZED":
                return HttpServletResponse.SC_UNAUTHORIZED;
            case "SUSPENDED":
                return HttpServletResponse.SC_FORBIDDEN;
            default:
                return HttpServletResponse.SC_FORBIDDEN;
        }
    }
}
