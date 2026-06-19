package com.kct.jcredit.core_backend.repository.dynamodb;

import com.kct.jcredit.core_backend.dto.request.OtherOperatorProjectsRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.Optional;

@Repository
public class OtherOperatorProjectsRepository {

    private final DynamoDbTable<OtherOperatorProjectsRequest> table;

    public OtherOperatorProjectsRepository(DynamoDbEnhancedClient enhancedClient,
                                           @Value("${app.dynamodb.table.project-plan}") String tableName) {
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(OtherOperatorProjectsRequest.class));
    }

    public OtherOperatorProjectsRequest save(OtherOperatorProjectsRequest request) {
        table.putItem(request);
        return request;
    }

    public Optional<OtherOperatorProjectsRequest> findByPkAndSk(String pk, String sk) {
        Key key = Key.builder()
                .partitionValue(pk)
                .sortValue(sk)
                .build();
        return Optional.ofNullable(table.getItem(key));
    }
}
