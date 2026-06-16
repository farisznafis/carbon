package com.kct.jcredit.core_backend.security;

import com.kct.jcredit.core_backend.entity.ParticipatingUser;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SiteAuthorization {

    private final ParticipatingUserRepository userRepository;
    private final SiteRepository siteRepository;
    private final JwtUtil jwtUtil;

    public SiteAuthorization(ParticipatingUserRepository userRepository,
                          SiteRepository siteRepository,
                          JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.siteRepository = siteRepository;
        this.jwtUtil = jwtUtil;
    }

    public boolean isUserAssignedToSite(int siteId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        String token = auth.getCredentials().toString();
        String username = jwtUtil.extractUsername(token);
        String companyId = jwtUtil.extractCompanyId(token);

        Optional<ParticipatingUser> userOpt = userRepository.findByUsernameAndCompanyId(username, companyId);
        if (userOpt.isEmpty()) {
            return false;
        }

        ParticipatingUser user = userOpt.get();
        return user.getSite() != null && user.getSite().getId() == siteId;
    }

    public Optional<Integer> getAssignedSiteId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }

        String token = auth.getCredentials().toString();
        String username = jwtUtil.extractUsername(token);
        String companyId = jwtUtil.extractCompanyId(token);

        Optional<ParticipatingUser> userOpt = userRepository.findByUsernameAndCompanyId(username, companyId);
        return userOpt.filter(u -> u.getSite() != null).map(u -> u.getSite().getId());
    }

    public boolean isUserAssignedToSiteOrMaster() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        String token = auth.getCredentials().toString();
        String role = jwtUtil.extractRole(token);

        if ("master".equals(role)) {
            return true;
        }

        return getAssignedSiteId().isPresent();
    }
}