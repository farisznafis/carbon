package com.kct.jcredit.core_backend.repository.dynamodb;

import com.kct.jcredit.core_backend.entity.EnR002ConfirmationItems; 

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
    private static final String TABLE_NAME = "en_r002_confirmation_items";


    // TABLE REF 

    private DynamoDbTable<EnR002ConfirmationItems> table() {
        return enhancedClient.table(TABLE_NAME, TableSchema.fromBean(EnR002ConfirmationItems.class));
    }

    // WRITE

    public void save(EnR002ConfirmationItems EnR002confirmationItems) {
        table().putItem(EnR002confirmationItems);
    }

    // READ

    public Optional<EnR002ConfirmationItems> findByPkAndSk(String pk, String sk) {
        Key key = Key.builder()
                .partitionValue(pk)
                .sortValue(sk)
                .build();
        return Optional.ofNullable(table().getItem(r -> r.key(key)));
    }

    // pk format: projectId#siteId
    public List<EnR002ConfirmationItems> findAllByProjectAndSite(Integer projectId, Integer siteId) {
        String pk = projectId + "#" + siteId;

        QueryConditional queryConditional = QueryConditional
                .keyEqualTo(Key.builder().partitionValue(pk).build());

        PageIterable<EnR002ConfirmationItems> pages = table().query(r -> r
                .queryConditional(queryConditional)
                .scanIndexForward(false) // DESC — terbaru dulu
        );

        return pages.items().stream().collect(Collectors.toList());
    }

    public Optional<EnR002ConfirmationItems> findLatestByProjectAndSite(Integer projectId, Integer siteId) {
        String pk = projectId + "#" + siteId;

        QueryConditional queryConditional = QueryConditional
                .keyEqualTo(Key.builder().partitionValue(pk).build());

        PageIterable<EnR002ConfirmationItems> pages = table().query(r -> r
                .queryConditional(queryConditional)
                .scanIndexForward(false)
                .limit(1)
        );

        return pages.items().stream().findFirst();
    }
}