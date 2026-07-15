package com.kct.jcredit.core_backend.repository.dynamodb;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import com.kct.jcredit.core_backend.dto.request.EnR002SustainabilityCommitmentRequest;
import com.kct.jcredit.core_backend.entity.EnR002SustainabilityCommitment;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EnR002SustainabilityCommitmentRepository {

    private final DynamoDbEnhancedClient enhancedClient;
    private static final String TABLE_NAME = "en_r002_sustainability_commitment";

    private DynamoDbTable<EnR002SustainabilityCommitment> table() {
        return enhancedClient.table(TABLE_NAME,
                TableSchema.fromBean(EnR002SustainabilityCommitment.class));
    }

    public void save(EnR002SustainabilityCommitment item) {
        table().putItem(item);
    }

    public Optional<EnR002SustainabilityCommitment> findByParticipationId(Integer participationId) {
        String pk = "PART#" + participationId;
        return Optional.ofNullable(table().getItem(r -> r.key(
                Key.builder().partitionValue(pk).build())));
    }
}
