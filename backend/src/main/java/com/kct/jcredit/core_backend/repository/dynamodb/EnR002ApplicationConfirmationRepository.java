package com.kct.jcredit.core_backend.repository.dynamodb;

import com.kct.jcredit.core_backend.entity.EnR002ApplicationConfirmation; 

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class EnR002ApplicationConfirmationRepository {

    private final DynamoDbEnhancedClient enhancedClient;
    private static final String TABLE_NAME = "en_r002_application_confirmation";

    private DynamoDbTable<EnR002ApplicationConfirmation> table() {
        return enhancedClient.table(TABLE_NAME,
                TableSchema.fromBean(EnR002ApplicationConfirmation.class));
    }

    public void save(EnR002ApplicationConfirmation item) {
        table().putItem(item);
    }

    public Optional<EnR002ApplicationConfirmation> findByParticipationId(Integer participationId) {
        String pk = "PART#" + participationId;
        return Optional.ofNullable(table().getItem(r -> r.key(
                Key.builder().partitionValue(pk).build())));
    }
}