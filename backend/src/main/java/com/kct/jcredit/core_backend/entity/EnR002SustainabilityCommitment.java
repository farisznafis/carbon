package com.kct.jcredit.core_backend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class EnR002SustainabilityCommitment {
    private String  pk; // "PART#{participationId}"
    private String  companyName;
    private String  repName;
    private String  repRole;
    private Boolean hasConsent;
    private Boolean isAgreeTerms;
    private String  committedAt;
    private String  createdBy;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("pk")
    public String getPk() { return pk; }

    @DynamoDbAttribute("company_name")
    public String getCompanyName() { return companyName; }

    @DynamoDbAttribute("rep_name")
    public String getRepName() { return repName; }

    @DynamoDbAttribute("rep_role")
    public String getRepRole() { return repRole; }

    @DynamoDbAttribute("has_consent")
    public Boolean getHasConsent() { return hasConsent; }

    @DynamoDbAttribute("is_agree_terms")
    public Boolean getIsAgreeTerms() { return isAgreeTerms; }

    @DynamoDbAttribute("committed_at")
    public String getCommittedAt() { return committedAt; }

    @DynamoDbAttribute("created_by")
    public String getCreatedBy() { return createdBy; }
}
