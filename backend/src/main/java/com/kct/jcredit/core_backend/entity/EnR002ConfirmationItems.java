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
public class EnR002ConfirmationItems {

    private String pk; // "PART#{participationId}"

    private Boolean isAgreeDataUse;
    private Boolean isAgreeExtraData;
    private Boolean isAgreeTransfer;
    private Boolean isNotRegistered;
    private Boolean isKeidanrenParticipation;
    private Boolean isEmitter;
    private String  emitterCode;
    private Boolean isAdjustedEmissionsAdded;
    private Boolean isSubjectToPeriodicReport;
    private Boolean isPeriodicReportTypeOne;
    private String  energyBusinessCode;
    private String  factoryCode;
    private String  agreedAt;
    private String  createdBy;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("pk")
    public String getPk() { return pk; }

    @DynamoDbAttribute("is_agree_data_use")
    public Boolean getIsAgreeDataUse() { return isAgreeDataUse; }

    @DynamoDbAttribute("is_agree_extra_data")
    public Boolean getIsAgreeExtraData() { return isAgreeExtraData; }

    @DynamoDbAttribute("is_agree_transfer")
    public Boolean getIsAgreeTransfer() { return isAgreeTransfer; }

    @DynamoDbAttribute("is_not_registered")
    public Boolean getIsNotRegistered() { return isNotRegistered; }

    @DynamoDbAttribute("is_keidanren_participation")
    public Boolean getIsKeidanrenParticipation() { return isKeidanrenParticipation; }

    @DynamoDbAttribute("is_emitter")
    public Boolean getIsEmitter() { return isEmitter; }

    @DynamoDbAttribute("emitter_code")
    public String getEmitterCode() { return emitterCode; }

    @DynamoDbAttribute("is_adjusted_emissions_added")
    public Boolean getIsAdjustedEmissionsAdded() { return isAdjustedEmissionsAdded; }

    @DynamoDbAttribute("is_subject_to_periodic_report")
    public Boolean getIsSubjectToPeriodicReport() { return isSubjectToPeriodicReport; }

    @DynamoDbAttribute("is_periodic_report_type_one")
    public Boolean getIsPeriodicReportTypeOne() { return isPeriodicReportTypeOne; }

    @DynamoDbAttribute("energy_business_code")
    public String getEnergyBusinessCode() { return energyBusinessCode; }

    @DynamoDbAttribute("factory_code")
    public String getFactoryCode() { return factoryCode; }

    @DynamoDbAttribute("agreed_at")
    public String getAgreedAt() { return agreedAt; }

    @DynamoDbAttribute("created_by")
    public String getCreatedBy() { return createdBy; }
}