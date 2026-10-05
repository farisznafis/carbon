package com.kct.jcredit.core_backend.repository.dynamodb;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecurityTempStateRepositoryTest {

    private static final Duration WINDOW = Duration.ofMinutes(15);

    private DynamoDbClient dynamoDbClient;
    private SecurityTempStateRepository repository;

    @BeforeEach
    void setUp() {
        dynamoDbClient = mock(DynamoDbClient.class);
        repository = new SecurityTempStateRepository(
                mock(DynamoDbEnhancedClient.class), dynamoDbClient, "security_temp_state");
    }

    @Test
    void incrementsWithinAnActiveWindow() {
        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class))).thenReturn(counterResponse(4L));

        assertThat(repository.incrementAndGet("rate#ip#abc", WINDOW)).isEqualTo(4L);
        verify(dynamoDbClient, times(1)).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    void startsAFreshWindowWhenTheStoredOneHasLapsed() {
        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class)))
                .thenThrow(ConditionalCheckFailedException.builder().message("no live window").build())
                .thenReturn(UpdateItemResponse.builder().build());

        assertThat(repository.incrementAndGet("rate#ip#abc", WINDOW)).isEqualTo(1L);
        verify(dynamoDbClient, times(2)).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    void joinsTheWindowAnotherCallerStartedFirst() {
        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class)))
                .thenThrow(ConditionalCheckFailedException.builder().message("no live window").build())
                .thenThrow(ConditionalCheckFailedException.builder().message("already started").build())
                .thenReturn(counterResponse(2L));

        assertThat(repository.incrementAndGet("rate#ip#abc", WINDOW)).isEqualTo(2L);
        verify(dynamoDbClient, times(3)).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    void surfacesDynamoFailuresAsServiceUnavailable() {
        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class)))
                .thenThrow(DynamoDbException.builder().message("throttled").build());

        assertThatThrownBy(() -> repository.incrementAndGet("rate#ip#abc", WINDOW))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ResponseConstants.SERVICE_UNAVAILABLE);
    }

    private UpdateItemResponse counterResponse(long value) {
        return UpdateItemResponse.builder()
                .attributes(Map.of("counter", AttributeValue.fromN(Long.toString(value))))
                .build();
    }
}
