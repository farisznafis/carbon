package com.kct.jcredit.core_backend.entity;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class ProjectDocumentStatus {

    private String projectId;
    private Boolean isRegistrationCertReceived;
    private Boolean isValidationReportReceived;
    private Boolean isVerificationReportReceived;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("projectId")
    public String getProjectId() {
        return projectId;
    }

    @DynamoDbAttribute("isRegistrationCertReceived")
    public Boolean getIsRegistrationCertReceived() {
        return isRegistrationCertReceived != null ? isRegistrationCertReceived : false;
    }

    @DynamoDbAttribute("isValidationReportReceived")
    public Boolean getIsValidationReportReceived() {
        return isValidationReportReceived != null ? isValidationReportReceived : false;
    }

    @DynamoDbAttribute("isVerificationReportReceived")
    public Boolean getIsVerificationReportReceived() {
        return isVerificationReportReceived != null ? isVerificationReportReceived : false;
    }
}