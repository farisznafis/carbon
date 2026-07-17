package com.kct.jcredit.core_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kct.jcredit.core_backend.config.SecurityFlowProperties;
import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.enums.OtpPurpose;
import com.kct.jcredit.core_backend.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OtpChallengeService {
    private static final String KEY_PREFIX = "security:otp:";
    private static final String SEND_PREFIX = "security:otp-send:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SecuritySecretService secretService;
    private final SecurityFlowProperties properties;

    public IssuedOtp create(OtpPurpose purpose, String subjectReference, String email) {
        String normalizedEmail = normalizeEmail(email);
        enforceSendLimit(purpose, normalizedEmail);
        UUID challengeId = UUID.randomUUID();
        String code = secretService.newOtp();
        long now = Instant.now().toEpochMilli();
        OtpData data = new OtpData(purpose, subjectReference, normalizedEmail,
                otpHash(purpose, challengeId, code), 0, 1, now, false);
        write(challengeId, data, properties.getOtpTtl());
        return new IssuedOtp(challengeId, code, properties.getOtpTtl().toSeconds(), masked(normalizedEmail), normalizedEmail);
    }

    public IssuedOtp resend(UUID challengeId, OtpPurpose expectedPurpose) {
        OtpData current = read(challengeId);
        if (current.purpose() != expectedPurpose) {
            throw invalidOtp();
        }
        long now = Instant.now().toEpochMilli();
        if (now - current.lastSentAtMillis() < properties.getOtpResendCooldown().toMillis()
                || current.sendCount() >= properties.getOtpMaxSends()) {
            throw tooManyRequests();
        }
        enforceSendLimit(current.purpose(), current.email());
        String code = secretService.newOtp();
        OtpData updated = new OtpData(current.purpose(), current.subjectReference(), current.email(),
                otpHash(current.purpose(), challengeId, code), current.failedAttempts(),
                current.sendCount() + 1, now, false);
        write(challengeId, updated, properties.getOtpTtl());
        return new IssuedOtp(challengeId, code, properties.getOtpTtl().toSeconds(), masked(current.email()), current.email());
    }

    public OtpData verify(UUID challengeId, String code) {
        OtpData current = read(challengeId);
        if (current.verified()) {
            return current;
        }
        if (current.failedAttempts() >= properties.getOtpMaxAttempts()) {
            delete(challengeId);
            throw invalidOtp();
        }
        String candidate = otpHash(current.purpose(), challengeId, code == null ? "" : code.trim());
        if (!secretService.constantTimeEquals(current.otpHash(), candidate)) {
            int failures = current.failedAttempts() + 1;
            if (failures >= properties.getOtpMaxAttempts()) {
                delete(challengeId);
            } else {
                OtpData updated = new OtpData(current.purpose(), current.subjectReference(), current.email(),
                        current.otpHash(), failures, current.sendCount(), current.lastSentAtMillis(), false);
                write(challengeId, updated, remainingTtl(challengeId));
            }
            throw invalidOtp();
        }
        OtpData verified = new OtpData(current.purpose(), current.subjectReference(), current.email(),
                current.otpHash(), current.failedAttempts(), current.sendCount(), current.lastSentAtMillis(), true);
        write(challengeId, verified, remainingTtl(challengeId));
        return verified;
    }

    public OtpData requireVerified(UUID challengeId, OtpPurpose purpose, String email) {
        OtpData data = read(challengeId);
        if (!data.verified() || data.purpose() != purpose || !data.email().equals(normalizeEmail(email))) {
            throw invalidOtp();
        }
        return data;
    }

    public OtpData consumeVerified(UUID challengeId, OtpPurpose purpose) {
        OtpData data = read(challengeId);
        if (!data.verified() || data.purpose() != purpose) {
            throw invalidOtp();
        }
        delete(challengeId);
        return data;
    }

    private void enforceSendLimit(OtpPurpose purpose, String normalizedEmail) {
        String key = SEND_PREFIX + purpose + ":" + secretService.hmac(normalizedEmail);
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, properties.getOtpSendWindow());
            }
            if (count != null && count > properties.getOtpMaxSends()) {
                throw tooManyRequests();
            }
        } catch (DataAccessException ex) {
            throw unavailable(ex);
        }
    }

    private OtpData read(UUID challengeId) {
        try {
            String json = redisTemplate.opsForValue().get(key(challengeId));
            if (json == null) {
                throw invalidOtp();
            }
            return objectMapper.readValue(json, OtpData.class);
        } catch (CustomException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            throw unavailable(ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid OTP challenge payload", ex);
        }
    }

    private void write(UUID challengeId, OtpData data, Duration ttl) {
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw invalidOtp();
        }
        try {
            redisTemplate.opsForValue().set(key(challengeId), objectMapper.writeValueAsString(data), ttl);
        } catch (DataAccessException ex) {
            throw unavailable(ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to serialize OTP challenge", ex);
        }
    }

    private Duration remainingTtl(UUID challengeId) {
        try {
            Long seconds = redisTemplate.getExpire(key(challengeId));
            return seconds == null || seconds <= 0 ? Duration.ZERO : Duration.ofSeconds(seconds);
        } catch (DataAccessException ex) {
            throw unavailable(ex);
        }
    }

    private void delete(UUID challengeId) {
        try {
            redisTemplate.delete(key(challengeId));
        } catch (DataAccessException ex) {
            throw unavailable(ex);
        }
    }

    private String otpHash(OtpPurpose purpose, UUID id, String code) {
        return secretService.hmac(purpose + ":" + id + ":" + code);
    }

    private String key(UUID challengeId) {
        return KEY_PREFIX + challengeId;
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new CustomException(ResponseConstants.BAD_REQUEST, ResponseConstants.MSG_BAD_REQUEST);
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String masked(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***" + email.substring(Math.max(at, 0));
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    private CustomException invalidOtp() {
        return new CustomException(ResponseConstants.INVALID_OR_EXPIRED_TOKEN,
                ResponseConstants.MSG_INVALID_OR_EXPIRED_TOKEN);
    }

    private CustomException tooManyRequests() {
        return new CustomException(ResponseConstants.TOO_MANY_REQUESTS, ResponseConstants.MSG_TOO_MANY_REQUESTS);
    }

    private CustomException unavailable(Exception cause) {
        return new CustomException(ResponseConstants.SERVICE_UNAVAILABLE,
                ResponseConstants.MSG_SERVICE_UNAVAILABLE, cause);
    }

    public record IssuedOtp(UUID challengeId, String code, long expiresInSeconds,
                            String maskedEmail, String email) {}

    public record OtpData(OtpPurpose purpose, String subjectReference, String email, String otpHash,
                          int failedAttempts, int sendCount, long lastSentAtMillis, boolean verified) {}
}
