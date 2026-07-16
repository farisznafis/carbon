package com.kct.jcredit.core_backend.security;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.constant.RoleConstants;
import com.kct.jcredit.core_backend.entity.OperatingUser;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingUser;
import com.kct.jcredit.core_backend.enums.CompanyStatus;
import com.kct.jcredit.core_backend.repository.postgres.OperatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
import com.kct.jcredit.core_backend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompanyAuthorizationFilter extends OncePerRequestFilter {

    private final ParticipatingCompanyRepository companyRepository;
    private final ParticipatingUserRepository userRepository;
    private final OperatingUserRepository operatingUserRepository;
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

        if (RoleConstants.OPERATOR.equals(role)) {
            authorizeOperator(request, response, filterChain, token, companyId, role);
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
     * Validates an operator against the operator domain without consulting participating companies.
     */
    private void authorizeOperator(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain,
            String token,
            String companyId,
            String tokenRole) throws IOException, ServletException {
        Integer tokenUserId = jwtUtil.extractUserId(token);
        String tokenUsername = jwtUtil.extractUsername(token);
        if (tokenUserId == null || tokenUsername == null) {
            log.warn(
                    "Operator access blocked. tokenUserIdPresent={}, tokenUsernamePresent={}, uri={}",
                    tokenUserId != null,
                    tokenUsername != null,
                    request.getRequestURI()
            );
            sendErrorResponse(response, ResponseConstants.FORBIDDEN, ResponseConstants.MSG_FORBIDDEN);
            return;
        }

        Optional<OperatingUser> operatorOpt = operatingUserRepository.findForAuthorization(
                tokenUsername,
                companyId
        );
        if (operatorOpt.isEmpty()) {
            log.warn(
                    "Operator access blocked. operatorDomainMatch=false, uri={}",
                    request.getRequestURI()
            );
            sendErrorResponse(response, ResponseConstants.FORBIDDEN, ResponseConstants.MSG_FORBIDDEN);
            return;
        }

        OperatingUser operator = operatorOpt.get();
        boolean userIdMatches = Objects.equals(tokenUserId, operator.getId());
        boolean roleMatches = RoleConstants.OPERATOR.equals(tokenRole)
                && RoleConstants.OPERATOR.equals(operator.getRole());
        boolean usernameMatches = Objects.equals(tokenUsername, operator.getUsername());
        boolean companyPresent = operator.getCompany() != null;
        boolean userActive = Boolean.TRUE.equals(operator.getIsActive());
        boolean companyActive = companyPresent && Boolean.TRUE.equals(operator.getCompany().getIsActive());
        boolean companyIdMatches = companyPresent
                && Objects.equals(companyId, operator.getCompany().getCompanyId());
        boolean authorized = userIdMatches
                && roleMatches
                && usernameMatches
                && userActive
                && companyActive
                && companyIdMatches;
        if (!authorized) {
            log.warn(
                    "Operator access blocked. userIdMatches={}, roleMatches={}, usernameMatches={}, "
                            + "userActive={}, companyPresent={}, companyActive={}, companyIdMatches={}, uri={}",
                    userIdMatches,
                    roleMatches,
                    usernameMatches,
                    userActive,
                    companyPresent,
                    companyActive,
                    companyIdMatches,
                    request.getRequestURI()
            );
            sendErrorResponse(response, ResponseConstants.FORBIDDEN, ResponseConstants.MSG_FORBIDDEN);
            return;
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
