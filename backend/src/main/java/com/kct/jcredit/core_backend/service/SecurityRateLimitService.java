package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class SecurityRateLimitService {
    private static final String PREFIX = "security:rate:";
    private final StringRedisTemplate redisTemplate;
    private final SecuritySecretService secretService;

    public void require(String bucket, String subject, int limit, Duration window) {
        String key = PREFIX + bucket + ":" + secretService.hmac(subject == null ? "" : subject);
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, window);
            }
            if (count != null && count > limit) {
                throw new CustomException(ResponseConstants.TOO_MANY_REQUESTS,
                        ResponseConstants.MSG_TOO_MANY_REQUESTS);
            }
        } catch (CustomException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            throw new CustomException(ResponseConstants.SERVICE_UNAVAILABLE,
                    ResponseConstants.MSG_SERVICE_UNAVAILABLE, ex);
        }
    }
}
