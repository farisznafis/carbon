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
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteResult;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;

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
    private final DynamoDbEnhancedClient enhancedClient;


    public MonitoringTableRecordSolarRepository(DynamoDbEnhancedClient enhancedClient) {
        this.enhancedClient = enhancedClient;
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

    public List<EnR002MonitoringRecord> findAllByPk(String pk) {
        QueryConditional qc = QueryConditional.keyEqualTo(k -> k.partitionValue(pk));
        PageIterable<EnR002MonitoringRecord> pages = table.query(r -> r.queryConditional(qc));

        List<EnR002MonitoringRecord> items = new ArrayList<>();
        for (Page<EnR002MonitoringRecord> page : pages) {
            items.addAll(page.items());
        }
        return items;
    }

    /**
     * Hapus banyak record sekaligus lewat DynamoDB BatchWriteItem (maks 25 item/request,
     * di-chunk otomatis). Return list Key yang GAGAL dihapus (unprocessed) setelah retry,
     * supaya caller tahu persis record mana yang perlu ditandai FAILURE di history.
     */
    public List<Key> deleteBatch(List<Key> keys) {
        List<Key> unprocessed = new ArrayList<>();
        for (List<Key> chunk : partition(keys, 25)) {
            WriteBatch.Builder<EnR002MonitoringRecord> batchBuilder =
                    WriteBatch.builder(EnR002MonitoringRecord.class).mappedTableResource(table);
            chunk.forEach(batchBuilder::addDeleteItem);

            BatchWriteResult result = enhancedClient.batchWriteItem(
                    BatchWriteItemEnhancedRequest.builder()
                            .writeBatches(batchBuilder.build())
                            .build());

            unprocessed.addAll(result.unprocessedDeleteItemsForTable(table));
        }
        return unprocessed;
    }

    private static List<List<Key>> partition(List<Key> keys, int size) {
        List<List<Key>> chunks = new ArrayList<>();
        for (int i = 0; i < keys.size(); i += size) {
            chunks.add(keys.subList(i, Math.min(i + size, keys.size())));
        }
        return chunks;
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
        List<String> targetPks,
        PaginationDetailResponse pagination) {

    if (targetPks == null || targetPks.isEmpty()) {
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

    // 2. Loop through target PKs (each PK = sitePublicId#projectDbId)
    while (siteIndex < targetPks.size() && result.size() < limit) {
        String pk = targetPks.get(siteIndex);
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
    if (siteIndex < targetPks.size()) {
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

    private List<EnR002MonitoringRecord> scanAllByProjectIdPaginated(Integer projectId, PaginationDetailResponse pagination) {
        int limit = pagination.getLimit();
        List<EnR002MonitoringRecord> result = new ArrayList<>();
        
        software.amazon.awssdk.enhanced.dynamodb.Expression filterExpression = software.amazon.awssdk.enhanced.dynamodb.Expression.builder()
            .expression("projectID = :projectId")
            .putExpressionValue(":projectId", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().n(String.valueOf(projectId)).build())
            .build();
            
        software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest.Builder scanBuilder = software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest.builder()
            .filterExpression(filterExpression)
            .limit(limit);
            
        if (pagination.getNextPageToken() != null && !pagination.getNextPageToken().isEmpty()) {
            try {
                String decodedStr = new String(java.util.Base64.getDecoder().decode(pagination.getNextPageToken()), java.nio.charset.StandardCharsets.UTF_8);
                String[] parts = decodedStr.split("::");
                if (parts.length == 2) {
                    Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue> startKey = new HashMap<>();
                    startKey.put("PK", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s(parts[0]).build());
                    startKey.put("SK", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s(parts[1]).build());
                    scanBuilder.exclusiveStartKey(startKey);
                }
            } catch (Exception e) {
                // ignore
            }
        }

        software.amazon.awssdk.enhanced.dynamodb.model.PageIterable<EnR002MonitoringRecord> pages = table.scan(scanBuilder.build());
        
        for (software.amazon.awssdk.enhanced.dynamodb.model.Page<EnR002MonitoringRecord> page : pages) {
            result.addAll(page.items());
            
            Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue> lastKey = page.lastEvaluatedKey();
            if (lastKey != null && !lastKey.isEmpty()) {
                String pk = lastKey.get("PK").s();
                String sk = lastKey.get("SK").s();
                String token = java.util.Base64.getEncoder().encodeToString((pk + "::" + sk).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                pagination.setNextPageToken(token);
                pagination.setHasMore(true);
            } else {
                pagination.setNextPageToken(null);
                pagination.setHasMore(false);
            }
            break;
        }
        
        return result;
    }

        /**
     * 指定した設備に紐づく最新のモニタリングレコードを取得する。
     * 同一拠点に複数設備がある場合でも、対象設備のレコードのみを返す。
     *
     * SK（timeUpdated）の降順で走査するため、最初に一致したものが最新となる。
     * findLatest とは異なり limit(1) を使用しない。先頭のレコードが対象設備のものとは限らないため。
     */
    public Optional<EnR002MonitoringRecord> findLatestByRecordIdAndEquipmentId(
            String recordId,
            Integer equipmentId
    ) {
        QueryConditional queryConditional = QueryConditional.keyEqualTo(
            Key.builder().partitionValue(recordId).build()
        );

        PageIterable<EnR002MonitoringRecord> pages = table.query(r -> r
            .queryConditional(queryConditional)
            .scanIndexForward(false) // DESC
        );

        return pages.items().stream()
            .filter(record -> Boolean.TRUE.equals(record.getIsActive()))
            .filter(record -> equipmentId != null
                && equipmentId.equals(record.getEquipmentID()))
            .findFirst();
    }
}
