package com.kct.jcredit.core_backend.repository.dynamodb;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.nio.charset.StandardCharsets;
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





public List<EnR002MonitoringRecord> findAllByProjectIdPaginated(
        Integer projectId,
        List<Integer> siteIds,
        PaginationDetailResponse pagination) {

    // 1. Get all sites connected to the project.
    // Replace ProjectSite with your actual site/entity class.
   // List<ProjectSite> sites = siteRepository.findAllByProjectId(projectId);

    if (siteIds == null || siteIds.isEmpty()) {
        pagination.setNextPageToken(null);
        pagination.setHasMore(false);
        return java.util.Collections.emptyList();
    }

    int limit = pagination.getLimit();
    List<EnR002MonitoringRecord> result = new ArrayList<>();

    ProjectPaginationToken token = decodeProjectPaginationToken(
        pagination.getNextPageToken()
    );

    int siteIndex = token.getSiteIndex();
    String lastSk = token.getLastSk();

    // 2. Loop through project sites in project/site order.
    while (siteIndex < siteIds.size() && result.size() < limit) {
        Integer siteId = siteIds.get(siteIndex);
        String pk = siteId + "#" + projectId;
        int remainingLimit = limit - result.size();

        QueryConditional queryConditional = QueryConditional.keyEqualTo(
            Key.builder()
                .partitionValue(pk)
                .build()
        );

        QueryEnhancedRequest.Builder requestBuilder = QueryEnhancedRequest.builder()
            .queryConditional(queryConditional)
            .scanIndexForward(false) // DESC by SK/time
            .limit(remainingLimit);

        // 3. If continuing inside the same site partition, resume from last SK.
        if (lastSk != null && !lastSk.isEmpty()) {
            Map<String, AttributeValue> exclusiveStartKey = new HashMap<>();
            exclusiveStartKey.put("PK", AttributeValue.builder().s(pk).build());
            exclusiveStartKey.put("SK", AttributeValue.builder().s(lastSk).build());

            requestBuilder.exclusiveStartKey(exclusiveStartKey);
        }

        PageIterable<EnR002MonitoringRecord> pages = table.query(requestBuilder.build());

        boolean currentSiteHasMore = false;
        String newLastSk = null;

        for (Page<EnR002MonitoringRecord> page : pages) {
            List<EnR002MonitoringRecord> pageItems = page.items();
            result.addAll(pageItems);

            Map<String, AttributeValue> lastEvaluatedKey = page.lastEvaluatedKey();

            if (lastEvaluatedKey != null && !lastEvaluatedKey.isEmpty()) {
                currentSiteHasMore = true;

                AttributeValue skValue = lastEvaluatedKey.get("SK");
                if (skValue != null) {
                    newLastSk = skValue.s();
                }
            }

            // Only consume one DynamoDB page at a time so pagination remains controlled.
            break;
        }

        if (currentSiteHasMore) {
            ProjectPaginationToken nextToken = new ProjectPaginationToken(
                siteIndex,
                newLastSk
            );

            pagination.setNextPageToken(encodeProjectPaginationToken(nextToken));
            pagination.setHasMore(true);

            return result;
        }

        // 4. Current site has no more records.
        // Move to the next site.
        siteIndex++;
        lastSk = null;
    }

    // 5. If we stopped because limit was reached exactly, but there are more sites left,
    // create a token pointing to the next site.
    if (siteIndex < siteIds.size()) {
        ProjectPaginationToken nextToken = new ProjectPaginationToken(
            siteIndex,
            null
        );

        pagination.setNextPageToken(encodeProjectPaginationToken(nextToken));
        pagination.setHasMore(true);
    } else {
        pagination.setNextPageToken(null);
        pagination.setHasMore(false);
    }

    return result;
}


    private static class ProjectPaginationToken {
        private int siteIndex;
        private String lastSk;

        public ProjectPaginationToken() {
        }

        public ProjectPaginationToken(int siteIndex, String lastSk) {
            this.siteIndex = siteIndex;
            this.lastSk = lastSk;
        }

        public int getSiteIndex() {
            return siteIndex;
        }

        public void setSiteIndex(int siteIndex) {
            this.siteIndex = siteIndex;
        }

        public String getLastSk() {
            return lastSk;
        }

        public void setLastSk(String lastSk) {
            this.lastSk = lastSk;
        }
    }
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String encodeProjectPaginationToken(ProjectPaginationToken token) {
        try {
            String json = objectMapper.writeValueAsString(token);

            return java.util.Base64.getEncoder()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));

        } catch (Exception e) {
            throw new RuntimeException("Failed to encode pagination token", e);
        }
    }

    private ProjectPaginationToken decodeProjectPaginationToken(String token) {
        if (token == null || token.isEmpty()) {
            return new ProjectPaginationToken(0, null);
        }

        try {
            String json = new String(
                java.util.Base64.getDecoder().decode(token),
                StandardCharsets.UTF_8
            );

            return objectMapper.readValue(json, ProjectPaginationToken.class);

        } catch (Exception e) {
            // If token is invalid, restart from beginning.
            return new ProjectPaginationToken(0, null);
        }
    }
}
