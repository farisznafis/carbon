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
import com.kct.jcredit.core_backend.entity.ParticipationReviewHistory;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ParticipationReviewHistoryRepository {

    private final DynamoDbEnhancedClient enhancedClient;
    private static final String TABLE_NAME = "participation_review_history";

    private DynamoDbTable<ParticipationReviewHistory> table() {
        return enhancedClient.table(TABLE_NAME, TableSchema.fromBean(ParticipationReviewHistory.class));
    }

    public void save(ParticipationReviewHistory item) {
        table().putItem(item);
    }

    public List<ParticipationReviewHistory> findByParticipationId(Integer participationId) {
        String pk = "PART#" + participationId;
        return table().query(r -> r.queryConditional(
                        QueryConditional.keyEqualTo(Key.builder().partitionValue(pk).build())))
                .items().stream()
                .collect(Collectors.toList());
    }

    public int countByParticipationId(Integer participationId) {
        return findByParticipationId(participationId).size();
    }
}
