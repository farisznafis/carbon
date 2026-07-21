package com.kct.jcredit.core_backend.repository.dynamodb;

import com.kct.jcredit.core_backend.entity.ProjectDocumentStatus;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import java.util.Optional;

@Repository
public class ProjectDocumentStatusRepository {

    private final DynamoDbTable<ProjectDocumentStatus> table;

    public ProjectDocumentStatusRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("project_document_status", TableSchema.fromBean(ProjectDocumentStatus.class));
    }

    public ProjectDocumentStatus save(ProjectDocumentStatus status) {
        table.putItem(status);
        return status;
    }

    public Optional<ProjectDocumentStatus> findById(String projectId) {
        Key key = Key.builder().partitionValue(projectId).build();
        return Optional.ofNullable(table.getItem(key));
    }
}