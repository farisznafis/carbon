package com.kct.jcredit.core_backend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class SecurityTempStateItem {

    private String pk;
    private String payload;
    private Long counter;
    private Long expiresAt;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("pk")
    public String getPk() {
        return pk;
    }

    @DynamoDbAttribute("payload")
    public String getPayload() {
        return payload;
    }

    @DynamoDbAttribute("counter")
    public Long getCounter() {
        return counter;
    }

    @DynamoDbAttribute("expiresAt")
    public Long getExpiresAt() {
        return expiresAt;
    }
}
