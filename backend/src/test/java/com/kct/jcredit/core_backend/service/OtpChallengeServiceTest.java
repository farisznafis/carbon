package com.kct.jcredit.core_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kct.jcredit.core_backend.config.SecurityFlowProperties;
import com.kct.jcredit.core_backend.enums.OtpPurpose;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.repository.dynamodb.SecurityTempStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

    @BeforeEach
    void setUp() {
        SecurityFlowProperties properties = new SecurityFlowProperties();
        properties.setTokenPepper("test-pepper-with-enough-randomness");
        SecurityTempStateRepository store = mock(SecurityTempStateRepository.class);
        doAnswer(invocation -> {
            values.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(store).put(anyString(), anyString(), any(Duration.class));
        when(store.get(anyString())).thenAnswer(invocation ->
                Optional.ofNullable(values.get(invocation.getArgument(0))));
        when(store.remainingTtl(anyString())).thenReturn(Duration.ofSeconds(300));
        when(store.incrementAndGet(anyString(), any(Duration.class))).thenReturn(1L);
        doAnswer(invocation -> {
            values.remove(invocation.getArgument(0));
            return null;
        }).when(store).delete(anyString());

        service = new OtpChallengeService(
                store,
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
