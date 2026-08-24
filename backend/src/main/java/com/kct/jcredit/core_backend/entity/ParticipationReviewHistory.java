package com.kct.jcredit.core_backend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@DynamoDbBean
@Builder
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor
public class ParticipationReviewHistory {

    private String pk;  // "PART#{participationId}"
    private String sk;  // "ATTEMPT#{attemptNumber}" — sort key, urut kronologis

    private Integer attemptNumber;
    private String  decision;              // "REJECTED"
    private String  rejectReason;
    private String  primaryReviewerName;
    private String  secondaryReviewerName;
    private Integer reviewedByOperatorId;
    private String  reviewedAt;            

    @DynamoDbPartitionKey
    @DynamoDbAttribute("pk")
    public String getPk() { return pk; }

    @DynamoDbSortKey
    @DynamoDbAttribute("sk")
    public String getSk() { return sk; }

    @DynamoDbAttribute("attempt_number")
    public Integer getAttemptNumber() { return attemptNumber; }

    @DynamoDbAttribute("decision")
    public String getDecision() { return decision; }

    @DynamoDbAttribute("reject_reason")
    public String getRejectReason() { return rejectReason; }

    @DynamoDbAttribute("primary_reviewer_name")
    public String getPrimaryReviewerName() { return primaryReviewerName; }

    @DynamoDbAttribute("secondary_reviewer_name")
    public String getSecondaryReviewerName() { return secondaryReviewerName; }

    @DynamoDbAttribute("reviewed_by_operator_id")
    public Integer getReviewedByOperatorId() { return reviewedByOperatorId; }

    @DynamoDbAttribute("reviewed_at")
    public String getReviewedAt() { return reviewedAt; }
}
