package com.kct.jcredit.core_backend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectParticipationConfirmation {
    private String pk; // "PART#{participationId}"

    private ReviewRecordParticipation primaryReview;   
    private Integer nominatedSecondaryReviewerId;
    private ReviewRecordParticipation secondaryReview; 

    @DynamoDbPartitionKey
    @DynamoDbAttribute("pk")
    public String getPk() { return pk; 
    }

    @DynamoDbAttribute("primary_review")
    public ReviewRecordParticipation getPrimaryReview() { 
        return primaryReview; 
    }
     @DynamoDbAttribute("nominated_secondary_reviewer_id")
    public Integer getNominatedSecondaryReviewerId() { return nominatedSecondaryReviewerId; }

    @DynamoDbAttribute("secondary_review")
    public ReviewRecordParticipation getSecondaryReview() { 
        return secondaryReview; 
    }
}