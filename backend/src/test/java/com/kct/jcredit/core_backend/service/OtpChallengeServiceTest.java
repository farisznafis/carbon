package com.kct.jcredit.core_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kct.jcredit.core_backend.config.SecurityFlowProperties;
import com.kct.jcredit.core_backend.enums.OtpPurpose;
import com.kct.jcredit.core_backend.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OtpChallengeServiceTest {
    private final Map<String, String> values = new HashMap<>();
    private OtpChallengeService service;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        SecurityFlowProperties properties = new SecurityFlowProperties();
        properties.setTokenPepper("test-pepper-with-enough-randomness");
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(operations);
        when(operations.increment(anyString())).thenReturn(1L);
        doAnswer(invocation -> {
            values.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(operations).set(anyString(), anyString(), any(Duration.class));
        when(operations.get(anyString())).thenAnswer(invocation -> values.get(invocation.getArgument(0)));
        when(redis.getExpire(anyString())).thenReturn(300L);
        when(redis.delete(anyString())).thenAnswer(invocation ->
                values.remove(invocation.getArgument(0)) != null);

        service = new OtpChallengeService(
                redis,
                new ObjectMapper(),
                new SecuritySecretService(properties),
                properties);
    }

    @Test
    void storesOtpAsHmacAndConsumesVerifiedChallenge() {
        OtpChallengeService.IssuedOtp issued = service.create(
                OtpPurpose.SIGNUP_EMAIL_VERIFICATION,
                "signup",
                "Person@Example.test");

        assertThat(issued.code()).matches("\\d{6}");
        assertThat(values.values()).noneMatch(value -> value.contains(issued.code()));

        OtpChallengeService.OtpData verified = service.verify(issued.challengeId(), issued.code());
        assertThat(verified.verified()).isTrue();
        assertThat(verified.email()).isEqualTo("person@example.test");

        service.consumeVerified(issued.challengeId(), OtpPurpose.SIGNUP_EMAIL_VERIFICATION);
        assertThat(values.keySet()).noneMatch(key -> key.endsWith(issued.challengeId().toString()));
    }

    @Test
    void rejectsIncorrectOtpWithoutMarkingChallengeVerified() {
        OtpChallengeService.IssuedOtp issued = service.create(
                OtpPurpose.PARTICIPATING_LOGIN,
                "P:1:0",
                "person@example.test");

        String wrongCode = "000000".equals(issued.code()) ? "999999" : "000000";
        assertThatThrownBy(() -> service.verify(issued.challengeId(), wrongCode))
                .isInstanceOf(CustomException.class);
        assertThat(values.values()).noneMatch(value -> value.contains("\"verified\":true"));
    }
}
