package com.kct.jcredit.core_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kct.jcredit.core_backend.config.SecurityFlowProperties;
import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.entity.SecurityActionLink;
import com.kct.jcredit.core_backend.enums.SecurityActionDeliveryStatus;
import com.kct.jcredit.core_backend.enums.SecurityActionPurpose;
import com.kct.jcredit.core_backend.enums.SecurityActionStatus;
import com.kct.jcredit.core_backend.enums.SecurityActionSubjectType;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.repository.postgres.SecurityActionLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SecurityActionLinkService {
    private static final String SESSION_PREFIX = "security:action-session:";
    private static final List<SecurityActionStatus> ACTIVE = List.of(
            SecurityActionStatus.PENDING, SecurityActionStatus.OPENED);

    private final SecurityActionLinkRepository repository;
    private final SecuritySecretService secretService;
    private final SecurityFlowProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public IssuedActionLink issue(SecurityActionPurpose purpose, SecurityActionSubjectType subjectType,
                                  Integer subjectId, String createdBy) {
        revokeActive(subjectType, subjectId, purpose);
        repository.flush();
        String rawToken = secretService.newActionToken();
        SecurityActionLink link = SecurityActionLink.builder()
                .id(UUID.randomUUID())
                .purpose(purpose)
                .subjectType(subjectType)
                .subjectId(subjectId)
                .tokenHash(secretService.sha256(rawToken))
                .status(SecurityActionStatus.PENDING)
                .deliveryStatus(SecurityActionDeliveryStatus.PENDING)
                .deliveryAttempts(0)
                .expiresAt(LocalDateTime.now().plus(linkTtl(purpose)))
                .createdAt(LocalDateTime.now())
                .createdBy(createdBy == null || createdBy.isBlank() ? "system" : createdBy)
                .build();
        repository.save(link);
        return new IssuedActionLink(link.getId(), rawToken, link.getExpiresAt(),
                buildFrontendUrl(purpose, rawToken));
    }

    @Transactional
    public ActionSession exchange(String rawToken) {
        SecurityActionLink found = repository
                .findByTokenHash(secretService.sha256(rawToken == null ? "" : rawToken))
                .orElseThrow(this::invalidToken);
        SecurityActionLink link = repository.findByIdForUpdate(found.getId()).orElseThrow(this::invalidToken);
        if (link.getStatus() != SecurityActionStatus.PENDING || !link.getExpiresAt().isAfter(LocalDateTime.now())) {
            if (link.getStatus() == SecurityActionStatus.PENDING
                    && !link.getExpiresAt().isAfter(LocalDateTime.now())) {
                link.setStatus(SecurityActionStatus.EXPIRED);
                repository.save(link);
            }
            throw invalidToken();
        }

        String sessionToken = secretService.newActionToken();
        Duration ttl = sessionTtl(link.getPurpose());
        ActionSessionData data = new ActionSessionData(
                link.getId(), link.getPurpose(), link.getSubjectType(), link.getSubjectId());
        writeSession(sessionToken, data, ttl);
        link.setStatus(SecurityActionStatus.OPENED);
        link.setOpenedAt(LocalDateTime.now());
        repository.save(link);
        return new ActionSession(sessionToken, link.getPurpose(), ttl.toSeconds());
    }

    public ActionSessionData requireSession(String sessionToken, SecurityActionPurpose expectedPurpose) {
        ActionSessionData data = readSession(sessionToken);
        if (data.purpose() != expectedPurpose) {
            throw invalidToken();
        }
        SecurityActionLink link = repository.findById(data.linkId()).orElseThrow(this::invalidToken);
        if (link.getStatus() != SecurityActionStatus.OPENED) {
            throw invalidToken();
        }
        return data;
    }

    @Transactional
    public void complete(String sessionToken, SecurityActionPurpose expectedPurpose) {
        ActionSessionData session = requireSession(sessionToken, expectedPurpose);
        SecurityActionLink link = repository.findByIdForUpdate(session.linkId()).orElseThrow(this::invalidToken);
        if (link.getStatus() != SecurityActionStatus.OPENED) {
            throw invalidToken();
        }
        link.setStatus(SecurityActionStatus.CONSUMED);
        link.setConsumedAt(LocalDateTime.now());
        repository.save(link);
        afterCommit(() -> redisTemplate.delete(sessionKey(sessionToken)));
    }

    @Transactional
    public void markDelivery(UUID linkId, boolean sent) {
        SecurityActionLink link = repository.findById(linkId).orElseThrow(this::invalidToken);
        link.setDeliveryAttempts(link.getDeliveryAttempts() + 1);
        link.setLastDeliveryAt(LocalDateTime.now());
        link.setDeliveryStatus(sent ? SecurityActionDeliveryStatus.SENT : SecurityActionDeliveryStatus.FAILED);
        repository.save(link);
    }

    @Scheduled(cron = "${app.security.action-link-cleanup-cron:0 20 3 * * *}")
    @Transactional
    public void cleanupTerminalLinks() {
        repository.expireActiveBefore(ACTIVE, SecurityActionStatus.EXPIRED, LocalDateTime.now());
        repository.deleteTerminalBefore(
                List.of(SecurityActionStatus.CONSUMED, SecurityActionStatus.REVOKED, SecurityActionStatus.EXPIRED),
                LocalDateTime.now().minusDays(properties.getTerminalRetentionDays()));
    }

    private void revokeActive(SecurityActionSubjectType subjectType, Integer subjectId,
                              SecurityActionPurpose purpose) {
        LocalDateTime now = LocalDateTime.now();
        repository.findBySubjectTypeAndSubjectIdAndPurposeAndStatusIn(subjectType, subjectId, purpose, ACTIVE)
                .forEach(link -> {
                    link.setStatus(SecurityActionStatus.REVOKED);
                    link.setRevokedAt(now);
                    repository.save(link);
                });
    }

    private Duration linkTtl(SecurityActionPurpose purpose) {
        return switch (purpose) {
            case MASTER_ACCOUNT_SETUP -> properties.getMasterSetupLinkTtl();
            case REJECTED_SIGNUP_REVISION -> properties.getRevisionLinkTtl();
            case PASSWORD_RESET -> properties.getPasswordResetLinkTtl();
        };
    }

    private Duration sessionTtl(SecurityActionPurpose purpose) {
        return switch (purpose) {
            case MASTER_ACCOUNT_SETUP -> properties.getMasterSetupSessionTtl();
            case REJECTED_SIGNUP_REVISION -> properties.getRevisionSessionTtl();
            case PASSWORD_RESET -> properties.getPasswordResetSessionTtl();
        };
    }

    private String buildFrontendUrl(SecurityActionPurpose purpose, String rawToken) {
        String path = switch (purpose) {
            case MASTER_ACCOUNT_SETUP -> "/account/setup";
            case REJECTED_SIGNUP_REVISION -> "/signup/revision";
            case PASSWORD_RESET -> "/reset-password";
        };
        return properties.getFrontendBaseUrl().replaceAll("/+$", "") + path + "#token=" + rawToken;
    }

    private void writeSession(String token, ActionSessionData data, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(sessionKey(token), objectMapper.writeValueAsString(data), ttl);
        } catch (DataAccessException ex) {
            throw unavailable(ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to serialize action session", ex);
        }
    }

    private ActionSessionData readSession(String token) {
        try {
            String json = redisTemplate.opsForValue().get(sessionKey(token));
            if (json == null) {
                throw invalidToken();
            }
            return objectMapper.readValue(json, ActionSessionData.class);
        } catch (CustomException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            throw unavailable(ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid action session payload", ex);
        }
    }

    private String sessionKey(String token) {
        return SESSION_PREFIX + secretService.hmac(token == null ? "" : token);
    }

    private void afterCommit(Runnable runnable) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runnable.run();
                }
            });
        } else {
            runnable.run();
        }
    }

    private CustomException invalidToken() {
        return new CustomException(ResponseConstants.INVALID_OR_EXPIRED_TOKEN,
                ResponseConstants.MSG_INVALID_OR_EXPIRED_TOKEN);
    }

    private CustomException unavailable(Exception cause) {
        return new CustomException(ResponseConstants.SERVICE_UNAVAILABLE,
                ResponseConstants.MSG_SERVICE_UNAVAILABLE, cause);
    }

    public record IssuedActionLink(UUID linkId, String rawToken, LocalDateTime expiresAt, String url) {}
    public record ActionSession(String sessionToken, SecurityActionPurpose purpose, long expiresInSeconds) {}
    public record ActionSessionData(UUID linkId, SecurityActionPurpose purpose,
                                    SecurityActionSubjectType subjectType, Integer subjectId) {}
}
