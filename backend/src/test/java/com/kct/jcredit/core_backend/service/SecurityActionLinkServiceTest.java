package com.kct.jcredit.core_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kct.jcredit.core_backend.config.SecurityFlowProperties;
import com.kct.jcredit.core_backend.entity.SecurityActionLink;
import com.kct.jcredit.core_backend.enums.SecurityActionPurpose;
import com.kct.jcredit.core_backend.enums.SecurityActionStatus;
import com.kct.jcredit.core_backend.enums.SecurityActionSubjectType;
import com.kct.jcredit.core_backend.repository.postgres.SecurityActionLinkRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import com.kct.jcredit.core_backend.enums.SecurityActionDeliveryStatus;
import com.kct.jcredit.core_backend.exception.CustomException;

import java.time.LocalDateTime;
import java.util.UUID;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecurityActionLinkServiceTest {

    @Test
    void issueStoresOnlyTokenHashAndBuildsFragmentUrl() {
        SecurityFlowProperties properties = new SecurityFlowProperties();
        properties.setFrontendBaseUrl("https://app.example.test/");
        properties.setTokenPepper("test-pepper-with-enough-randomness");
        SecuritySecretService secretService = new SecuritySecretService(properties);
        SecurityActionLinkRepository repository = mock(SecurityActionLinkRepository.class);
        when(repository.findBySubjectTypeAndSubjectIdAndPurposeAndStatusIn(
                any(), any(), any(), any())).thenReturn(List.of());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SecurityActionLinkService service = new SecurityActionLinkService(
                repository,
                secretService,
                properties,
                mock(StringRedisTemplate.class),
                new ObjectMapper());

        SecurityActionLinkService.IssuedActionLink issued = service.issue(
                SecurityActionPurpose.PASSWORD_RESET,
                SecurityActionSubjectType.PARTICIPATING_USER,
                42,
                "operator@example.test");

        ArgumentCaptor<SecurityActionLink> captor = ArgumentCaptor.forClass(SecurityActionLink.class);
        verify(repository).save(captor.capture());
        SecurityActionLink stored = captor.getValue();
        assertThat(stored.getTokenHash()).hasSize(64).isNotEqualTo(issued.rawToken());
        assertThat(stored.getStatus()).isEqualTo(SecurityActionStatus.PENDING);
        assertThat(issued.url()).startsWith("https://app.example.test/reset-password#token=");
        assertThat(issued.url()).endsWith(issued.rawToken());
    }

    @Test
    @SuppressWarnings("unchecked")
    void exchangedEmailTokenCannotBeExchangedTwice() {
        SecurityFlowProperties properties = new SecurityFlowProperties();
        properties.setTokenPepper("test-pepper-with-enough-randomness");
        SecuritySecretService secretService = new SecuritySecretService(properties);
        SecurityActionLinkRepository repository = mock(SecurityActionLinkRepository.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(operations);

        String rawToken = secretService.newActionToken();
        SecurityActionLink link = SecurityActionLink.builder()
                .id(UUID.randomUUID())
                .purpose(SecurityActionPurpose.PASSWORD_RESET)
                .subjectType(SecurityActionSubjectType.PARTICIPATING_USER)
                .subjectId(42)
                .tokenHash(secretService.sha256(rawToken))
                .status(SecurityActionStatus.PENDING)
                .deliveryStatus(SecurityActionDeliveryStatus.SENT)
                .deliveryAttempts(1)
                .createdAt(LocalDateTime.now())
                .createdBy("operator")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();
        when(repository.findByTokenHash(link.getTokenHash())).thenReturn(java.util.Optional.of(link));
        when(repository.findByIdForUpdate(link.getId())).thenReturn(java.util.Optional.of(link));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SecurityActionLinkService service = new SecurityActionLinkService(
                repository, secretService, properties, redis, new ObjectMapper());

        SecurityActionLinkService.ActionSession session = service.exchange(rawToken);
        assertThat(session.purpose()).isEqualTo(SecurityActionPurpose.PASSWORD_RESET);
        assertThat(link.getStatus()).isEqualTo(SecurityActionStatus.OPENED);
        assertThatThrownBy(() -> service.exchange(rawToken)).isInstanceOf(CustomException.class);
    }
}
