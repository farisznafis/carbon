package com.kct.jcredit.core_backend.repository.dynamodb;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.entity.SecurityTempStateItem;
import com.kct.jcredit.core_backend.exception.CustomException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Short-lived security state (OTP challenges, send counters, rate-limit counters, action sessions).
 *
 * <p>DynamoDB TTL reaps {@code expiresAt} on a best-effort basis and may lag by up to 48 hours,
 * so every read validates {@code expiresAt} itself and treats a lapsed item as absent.
 */
@Repository
public class SecurityTempStateRepository {

    private final DynamoDbTable<SecurityTempStateItem> table;
    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public SecurityTempStateRepository(DynamoDbEnhancedClient enhancedClient,
                                       DynamoDbClient dynamoDbClient,
                                       @Value("${app.dynamodb.table.security-temp-state}") String tableName) {
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(SecurityTempStateItem.class));
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    public void put(String pk, String payload, Duration ttl) {
        try {
            table.putItem(SecurityTempStateItem.builder()
                    .pk(pk)
                    .payload(payload)
                    .expiresAt(nowEpochSeconds() + ttl.toSeconds())
                    .build());
        } catch (SdkException ex) {
            throw unavailable(ex);
        }
    }

    public Optional<String> get(String pk) {
        return findLive(pk).map(SecurityTempStateItem::getPayload);
    }

    public Duration remainingTtl(String pk) {
        return findLive(pk)
                .map(item -> Duration.ofSeconds(item.getExpiresAt() - nowEpochSeconds()))
                .orElse(Duration.ZERO);
    }

    public void delete(String pk) {
        try {
            table.deleteItem(Key.builder().partitionValue(pk).build());
        } catch (SdkException ex) {
            throw unavailable(ex);
        }
    }

    /**
     * Atomic counter with a window that starts at the first request, mirroring Redis {@code INCR} +
     * {@code EXPIRE}-on-first-write.
     *
     * <p>Restarting the window when the stored one has lapsed is mandatory: TTL reaping lags, so an
     * expired counter item is still readable and an unconditional {@code ADD} would keep incrementing
     * it and lock the subject out long past the window.
     */
    public long incrementAndGet(String pk, Duration window) {
        try {
            return incrementWithinWindow(pk);
        } catch (ConditionalCheckFailedException ex) {
            // No live window - fall through and start one.
        } catch (SdkException ex) {
            throw unavailable(ex);
        }
        try {
            return startWindow(pk, window);
        } catch (ConditionalCheckFailedException ex) {
            // Another caller started the window first - fall through and join it.
        } catch (SdkException ex) {
            throw unavailable(ex);
        }
        try {
            return incrementWithinWindow(pk);
        } catch (SdkException ex) {
            throw unavailable(ex);
        }
    }

    private long incrementWithinWindow(String pk) {
        UpdateItemResponse response = dynamoDbClient.updateItem(UpdateItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("pk", AttributeValue.fromS(pk)))
                .updateExpression("ADD #counter :one")
                .conditionExpression("attribute_exists(pk) AND #expiresAt > :now")
                .expressionAttributeNames(Map.of("#counter", "counter", "#expiresAt", "expiresAt"))
                .expressionAttributeValues(Map.of(
                        ":one", numeric(1L),
                        ":now", numeric(nowEpochSeconds())))
                .returnValues(ReturnValue.UPDATED_NEW)
                .build());
        AttributeValue counter = response.attributes().get("counter");
        return counter == null ? 1L : Long.parseLong(counter.n());
    }

    private long startWindow(String pk, Duration window) {
        long now = nowEpochSeconds();
        dynamoDbClient.updateItem(UpdateItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("pk", AttributeValue.fromS(pk)))
                .updateExpression("SET #counter = :one, #expiresAt = :expiresAt")
                .conditionExpression("attribute_not_exists(pk) OR #expiresAt <= :now")
                .expressionAttributeNames(Map.of("#counter", "counter", "#expiresAt", "expiresAt"))
                .expressionAttributeValues(Map.of(
                        ":one", numeric(1L),
                        ":now", numeric(now),
                        ":expiresAt", numeric(now + window.toSeconds())))
                .build());
        return 1L;
    }

    private Optional<SecurityTempStateItem> findLive(String pk) {
        SecurityTempStateItem item;
        try {
            item = table.getItem(request -> request
                    .key(Key.builder().partitionValue(pk).build())
                    .consistentRead(true));
        } catch (SdkException ex) {
            throw unavailable(ex);
        }
        if (item == null || item.getExpiresAt() == null) {
            return Optional.empty();
        }
        if (item.getExpiresAt() <= nowEpochSeconds()) {
            deleteQuietly(pk);
            return Optional.empty();
        }
        return Optional.of(item);
    }

    private void deleteQuietly(String pk) {
        try {
            table.deleteItem(Key.builder().partitionValue(pk).build());
        } catch (SdkException ex) {
            // The item is already unusable; leave it for TTL to reap.
        }
    }

    private AttributeValue numeric(long value) {
        return AttributeValue.fromN(Long.toString(value));
    }

    private long nowEpochSeconds() {
        return Instant.now().getEpochSecond();
    }

    private CustomException unavailable(Exception cause) {
        return new CustomException(ResponseConstants.SERVICE_UNAVAILABLE,
                ResponseConstants.MSG_SERVICE_UNAVAILABLE, cause);
    }
}
