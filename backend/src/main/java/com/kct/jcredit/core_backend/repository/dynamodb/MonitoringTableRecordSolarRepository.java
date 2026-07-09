package com.kct.jcredit.core_backend.repository.dynamodb;
import com.kct.jcredit.core_backend.entity.EnR002MonitoringRecord;
import com.kct.jcredit.core_backend.entity.base.PaginationDetailResponse;

import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

import java.util.Optional;

@Repository
public class MonitoringTableRecordSolarRepository {

    private final DynamoDbTable<EnR002MonitoringRecord> table;


    public MonitoringTableRecordSolarRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table(
            "solar_monitoring_table_record",
            TableSchema.fromBean(EnR002MonitoringRecord.class)
        );
    }

    public EnR002MonitoringRecord save(EnR002MonitoringRecord record) {
        table.putItem(record);
        return record;
    }

    public Optional<EnR002MonitoringRecord > findByPKandSK(String pk, String sk) {
        Key key = Key.builder()
                .partitionValue(pk)
                .sortValue(sk)
                .build();
        
        EnR002MonitoringRecord item = table.getItem(key);
        return Optional.ofNullable(item);
    }

    public List<EnR002MonitoringRecord> findByPkAndSkBetween(String pk, String startSk, String endSk) {
        QueryConditional qc = QueryConditional
            .sortBetween(
                Key.builder()
                    .partitionValue(pk)
                    .sortValue(startSk)
                    .build(),
                Key.builder()
                    .partitionValue(pk)
                    .sortValue(endSk)
                    .build()
            );
            
        PageIterable<EnR002MonitoringRecord> pages = table.query(r -> r
            .queryConditional(qc)
            .scanIndexForward(true) // ASC by time
        );
        
        return pages.items().stream().collect(Collectors.toList());
    }


    public void delete(String pk, String sk) {
        Key key = Key.builder()
                .partitionValue(pk)
                .sortValue(sk)
                .build();
        
        table.deleteItem(key);
    }

    public Optional<EnR002MonitoringRecord> findLatest(String pk) {
        QueryConditional qc = QueryConditional.keyEqualTo(k -> k.partitionValue(pk));
        PageIterable<EnR002MonitoringRecord> pages = table.query(r -> r
            .queryConditional(qc)
            .scanIndexForward(false) // DESC
            .limit(1)
        );
        return pages.items().stream().findFirst();
    }

    public boolean existsActiveByRecordIdAndDateRecorded(String recordId, String dateRecorded) {
        QueryConditional queryConditional = QueryConditional.keyEqualTo(
            Key.builder().partitionValue(recordId).build()
        );

        PageIterable<EnR002MonitoringRecord> pages = table.query(r -> r
            .queryConditional(queryConditional)
            .scanIndexForward(false)
        );

        return pages.items().stream()
            .anyMatch(record -> Boolean.TRUE.equals(record.getIsActive())
                && dateRecorded != null
                && dateRecorded.equals(record.getDateRecorded()));
    }

public List<EnR002MonitoringRecord> findAllByRecordIdPaginated(
        String recordId, 
        PaginationDetailResponse pagination) {
    
    QueryConditional queryConditional = QueryConditional.keyEqualTo(
        Key.builder().partitionValue(recordId).build()
    );
    
    var queryEnhancedRequestBuilder = QueryEnhancedRequest.builder()
        .queryConditional(queryConditional)
        .scanIndexForward(false)
        .limit(pagination.getLimit());
    
    if (pagination.getNextPageToken() != null && !pagination.getNextPageToken().isEmpty()) {
        try {
            // Decode the timestamp string
            String decodedTimestamp = new String(
                java.util.Base64.getDecoder().decode(pagination.getNextPageToken())
            );
            
            // Build exclusiveStartKey with BOTH keys
            Map<String, AttributeValue> exclusiveStartKey = new HashMap<>();
            exclusiveStartKey.put("PK", AttributeValue.builder().s(recordId).build());
            exclusiveStartKey.put("SK", AttributeValue.builder().s(decodedTimestamp).build());  // ✅ String型
            
            queryEnhancedRequestBuilder.exclusiveStartKey(exclusiveStartKey);
            
        } catch (Exception e) {
            System.err.println("Token decode error: " + e.getMessage());
        }
    }
    
    PageIterable<EnR002MonitoringRecord> pages = table.query(queryEnhancedRequestBuilder.build());
    List<EnR002MonitoringRecord> items = new ArrayList<>();

    for (var page : pages) {
        List<EnR002MonitoringRecord> pageItems = page.items();
        items.addAll(pageItems);
        
        Map<String, AttributeValue> lastKey = page.lastEvaluatedKey();
        
        if (lastKey != null && !lastKey.isEmpty()) {
            if (!pageItems.isEmpty()) {
                // Get timeUpdated from last item
                EnR002MonitoringRecord lastItem = pageItems.get(pageItems.size() - 1);
                String lastTimestamp = lastItem.getTimeUpdated();
                
                if (lastTimestamp != null) {
                    String nextToken = java.util.Base64.getEncoder()
                        .encodeToString(lastTimestamp.getBytes());
                    
                    pagination.setNextPageToken(nextToken);
                    pagination.setHasMore(true);
                    
                } else {
                    pagination.setNextPageToken(null);
                    pagination.setHasMore(false);
                }
            }
        } else {
            pagination.setNextPageToken(null);
            pagination.setHasMore(false);
        }
        
        break;
    }

    return items;
}
}
