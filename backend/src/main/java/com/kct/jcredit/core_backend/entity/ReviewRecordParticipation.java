package com.kct.jcredit.core_backend.entity;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
@Builder
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor
public class ReviewRecordParticipation {

    private Integer reviewerId;
    private String  reviewerName;
    private String  reviewerInkan;
    private String  decision;       
    private String  rejectReason;   
    private Map<String, Boolean> docChecks;      
    private Map<String, Boolean> dataMgmtChecks; 
    private String  reviewedAt;
}
