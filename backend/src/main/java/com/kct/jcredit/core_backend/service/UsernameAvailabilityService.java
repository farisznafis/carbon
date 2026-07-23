package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.exception.DuplicateResourceException;
import com.kct.jcredit.core_backend.repository.postgres.OperatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UsernameAvailabilityService {
    private final ParticipatingUserRepository participatingUserRepository;
    private final OperatingUserRepository operatingUserRepository;
    private final EntityManager entityManager;

    public String lockAndRequireAvailable(String username) {
        String normalized = normalize(username);
        entityManager.createNativeQuery(
                        "SELECT pg_advisory_xact_lock(hashtextextended(?1, 0))")
                .setParameter(1, normalized)
                .getSingleResult();
        if (participatingUserRepository.existsByUsernameIgnoreCase(normalized)
                || operatingUserRepository.existsByUsernameIgnoreCase(normalized)) {
            throw new DuplicateResourceException(
                    ResponseConstants.USED_USERNAME,
                    ResponseConstants.MSG_USED_USERNAME);
        }
        return username.trim();
    }

    public boolean isAvailable(String username) {
        String normalized = normalize(username);
        return !participatingUserRepository.existsByUsernameIgnoreCase(normalized)
                && !operatingUserRepository.existsByUsernameIgnoreCase(normalized);
    }

    private String normalize(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username is required");
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
