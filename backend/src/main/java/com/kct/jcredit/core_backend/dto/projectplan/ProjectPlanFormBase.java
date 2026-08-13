package com.kct.jcredit.core_backend.dto.projectplan;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/**
 * Keys and audit metadata shared by every project-plan page.
 *
 * <p>Deliberately <b>not</b> annotated {@code @DynamoDbBean} — only concrete
 * subclasses carry that annotation. {@code TableSchema.fromBean} introspects
 * inherited getters, so the key and metadata mappings declared here are picked
 * up on every subclass.
 *
 * <p>Uses {@code @Getter}/{@code @Setter} rather than {@code @Data} so that
 * subclasses annotated {@code @Data} do not inherit a conflicting
 * {@code equals}/{@code hashCode} contract.
 *
 * <p>Subclasses must declare {@code @EqualsAndHashCode(callSuper = true)} and
 * must <b>not</b> use {@code @Builder}/{@code @AllArgsConstructor}: Lombok's
 * builder on a subclass silently omits superclass fields, which would leave
 * {@code pk}/{@code sk} unsettable with no compile error.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class ProjectPlanFormBase {

    @JsonIgnore
    private String pk;

    @JsonIgnore
    private String sk;

    /**
     * Server-managed. Serialized to clients but ignored on deserialize, so a
     * client cannot forge completion by posting {@code "status":"COMPLETE"}.
     *
     * <p>NOTE: {@code status} is a DynamoDB reserved word. It is safe for
     * putItem/getItem/key conditions, but any future filter expression or
     * projection must escape it as {@code #status}.
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String status;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Instant updatedAt;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String updatedBy;

    /** Files attached to this page. Permanent S3 keys only. */
    @JsonProperty("attachments")
    private List<FormAttachment> attachments;

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

    @DynamoDbAttribute("attachments")
    public List<FormAttachment> getAttachments() {
        return attachments;
    }
}
