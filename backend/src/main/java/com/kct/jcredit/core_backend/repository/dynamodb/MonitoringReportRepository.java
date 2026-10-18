package com.kct.jcredit.core_backend.repository.dynamodb;

import com.kct.jcredit.core_backend.dto.request.MonitoringReportRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;

import java.util.Optional;

@Repository
public class MonitoringReportRepository {

    private final DynamoDbTable<MonitoringReportRequest> table;

    public MonitoringReportRepository(DynamoDbEnhancedClient enhancedClient,
                                       @Value("${app.dynamodb.table.monitoring-report}") String tableName) {
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(MonitoringReportRequest.class));
    }

    public MonitoringReportRequest save(MonitoringReportRequest request) {
        table.putItem(request);
        return request;
    }

    public Optional<MonitoringReportRequest> findLatestByPk(String pk) {
        QueryConditional condition = QueryConditional.keyEqualTo(Key.builder().partitionValue(pk).build());
        PageIterable<MonitoringReportRequest> pages = table.query(r -> r
                .queryConditional(condition)
                .scanIndexForward(false)
                .limit(1));
        return pages.items().stream().findFirst();
    }
}
