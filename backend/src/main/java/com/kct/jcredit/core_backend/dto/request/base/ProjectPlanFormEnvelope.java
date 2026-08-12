package com.kct.jcredit.core_backend.dto.request.base;

import java.time.Instant;

import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/**
 * Metadata-only read model over the shared project-plan table.
 *
 * <p>Lets the progress endpoint pull every page of a project in a <b>single</b>
 * Query instead of one getItem per page. Reading heterogeneous items through
 * this narrow schema is safe: {@code StaticImmutableTableSchema.mapToItem}
 * iterates the stored item's attributes and silently discards any that the
 * schema does not map, so each page's payload fields are simply ignored.
 *
 * <p>Standalone rather than extending {@link ProjectPlanFormBase} so it stays a
 * pure read model with no payload or JSON concerns.
 */
@Data
@NoArgsConstructor
@DynamoDbBean
public class ProjectPlanFormEnvelope {

    private String pk;
    private String sk;
    private String status;
    private Instant updatedAt;
    private String updatedBy;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("PK")
    public String getPk() {
        return pk;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("SK")
    public String getSk() {
        return sk;
    }

    @DynamoDbAttribute("status")
    public String getStatus() {
        return status;
    }

    @DynamoDbAttribute("updatedAt")
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @DynamoDbAttribute("updatedBy")
    public String getUpdatedBy() {
        return updatedBy;
    }
}
