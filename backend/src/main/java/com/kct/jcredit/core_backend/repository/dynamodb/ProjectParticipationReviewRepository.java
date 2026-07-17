package com.kct.jcredit.core_backend.repository.dynamodb;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import com.kct.jcredit.core_backend.entity.ProjectParticipationReview;

@Repository
@RequiredArgsConstructor
public class ProjectParticipationReviewRepository {
    private final DynamoDbEnhancedClient enhancedClient;
    private static final String TABLE_NAME = "project_participation_confirmation";

    private DynamoDbTable<ProjectParticipationReview> table() {
        return enhancedClient.table(TABLE_NAME,
                TableSchema.fromBean(ProjectParticipationReview.class));
    }

    public void save(ProjectParticipationReview  item) {
        table().putItem(item);
    }

    public Optional<ProjectParticipationReview> findByParticipationId(Integer participationId) {
        String pk = "PART#" + participationId;
        return Optional.ofNullable(table().getItem(r -> r.key(
                Key.builder().partitionValue(pk).build())));
    }
}