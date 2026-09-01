package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.repository.dynamodb.SecurityTempStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class SecurityRateLimitService {
    private static final String PREFIX = "rate#";
    private final SecurityTempStateRepository store;
    private final SecuritySecretService secretService;

    public void require(String bucket, String subject, int limit, Duration window) {
        String key = PREFIX + bucket + "#" + secretService.hmac(subject == null ? "" : subject);
        if (store.incrementAndGet(key, window) > limit) {
            throw new CustomException(ResponseConstants.TOO_MANY_REQUESTS,
                    ResponseConstants.MSG_TOO_MANY_REQUESTS);
        }
    }
}
