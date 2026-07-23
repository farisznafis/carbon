package com.kct.jcredit.core_backend.security;

import com.kct.jcredit.core_backend.config.SecurityFlowProperties;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.service.SecurityRateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
public class SecurityEndpointRateLimitFilter extends OncePerRequestFilter {
    private final SecurityRateLimitService rateLimitService;
    private final SecurityFlowProperties properties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!isSecurityMutation(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        String client = resolveClientAddress(request);
        try {
            rateLimitService.require("ip", client, properties.getIpMaxRequests(), properties.getIpRateWindow());
            filterChain.doFilter(request, response);
        } catch (CustomException ex) {
            response.setStatus("TOO_MANY_REQUESTS".equals(ex.getErrorCode()) ? 429 : 503);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"statusCode\":\"" + ex.getErrorCode()
                    + "\",\"message\":\"" + ex.getMessage() + "\"}");
        }
    }

    private boolean isSecurityMutation(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod()) && !"PUT".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String uri = request.getRequestURI();
        boolean actionLink = uri.contains("/auth/action-links/")
                && (properties.getFeatures().isMasterSetupLink()
                    || properties.getFeatures().isRejectedSignupRevision()
                    || properties.getFeatures().isPasswordReset());
        boolean masterSetup = properties.getFeatures().isMasterSetupLink()
                && (uri.contains("/auth/master-account/") || uri.contains("/master-setup-link"));
        boolean passwordReset = properties.getFeatures().isPasswordReset()
                && (uri.contains("/auth/password-reset/") || uri.contains("/password-reset-link"));
        boolean loginOtp = properties.getFeatures().isLoginEmailOtp()
                && (uri.contains("/auth/email-otp/") || uri.contains("/login/email-otp/"));
        boolean passwordLogin = properties.getFeatures().isLoginEmailOtp()
                && (uri.endsWith("/auth/user/login") || uri.endsWith("/auth/admin/login"));
        boolean signupVerification = properties.getFeatures().isSignupEmailVerification()
                && uri.contains("/company/signup/email-verification");
        boolean revision = properties.getFeatures().isRejectedSignupRevision()
                && uri.contains("/company/signup/revision");
        return actionLink || masterSetup || passwordReset || loginOtp || passwordLogin
                || signupVerification || revision;
    }

    private String resolveClientAddress(HttpServletRequest request) {
        // Do not trust client-supplied forwarding headers here. A trusted reverse proxy
        // should normalize the remote address before the request reaches the application.
        return request.getRemoteAddr();
    }
}
