package com.kct.jcredit.core_backend.security;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.constant.RoleConstants;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingUser;
import com.kct.jcredit.core_backend.enums.CompanyStatus;
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

    /**
     * Enforces canonical company access status before protected business endpoints run.
     */
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
        if (company.getCompanyStatus() != CompanyStatus.ACTIVE) {
            log.warn(
                    "Company access blocked. companyId={}, status={}, uri={}",
                    company.getCompanyId(),
                    company.getCompanyStatus(),
                    request.getRequestURI()
            );
            String statusCode = company.getCompanyStatus() == CompanyStatus.SUSPENDED
                    ? ResponseConstants.SUSPENDED
                    : ResponseConstants.FORBIDDEN;
            String message = company.getCompanyStatus() == CompanyStatus.SUSPENDED
                    ? ResponseConstants.MSG_COMPANY_ACCESS_SUSPENDED
                    : ResponseConstants.MSG_FORBIDDEN;
            sendErrorResponse(response, statusCode, message);
            return;
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

    /**
     * Skips public authentication documentation and login routes.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/auth/") || path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui");
    }

    /**
     * Writes the existing compact security error contract.
     */
    private void sendErrorResponse(HttpServletResponse response, String statusCode, String message) throws IOException {
        response.setStatus(mapStatusCodeToHttpStatus(statusCode));
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
            "{\"statusCode\":\"" + statusCode + "\",\"message\":\"" + message + "\"}"
        );
    }

    /**
     * Maps internal security status codes to HTTP status codes.
     */
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
