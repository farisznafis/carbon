package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.entity.OperatingUser;
import com.kct.jcredit.core_backend.entity.ParticipatingUser;
import com.kct.jcredit.core_backend.repository.postgres.OperatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthVersionService {
    private final ParticipatingUserRepository participatingUserRepository;
    private final OperatingUserRepository operatingUserRepository;

    @Transactional(readOnly = true)
    public int current(Integer userId, String role) {
        if ("operator".equals(role)) {
            return operatingUserRepository.findById(userId)
                    .map(OperatingUser::getAuthVersion)
                    .orElse(-1);
        }
        return participatingUserRepository.findById(userId)
                .map(ParticipatingUser::getAuthVersion)
                .orElse(-1);
    }

    public void increment(ParticipatingUser user) {
        user.setAuthVersion(safe(user.getAuthVersion()) + 1);
        participatingUserRepository.save(user);
    }

    public void increment(OperatingUser user) {
        user.setAuthVersion(safe(user.getAuthVersion()) + 1);
        operatingUserRepository.save(user);
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }
}
