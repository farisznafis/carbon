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
public class TncAgreementEquip {

    private String pk;
    private String sk;
    private Integer projectId;
    private Integer siteId;
    private String companyId;
    private Integer participationId;
    private Boolean isAgreeDataUse;
    private Boolean isAgreeExtraData;
    private Boolean isAgreeTransfer;
    private Boolean isNotRegistered;
    private Boolean isKeidanrenParticipation;
    private Boolean isEmitter;
    private Boolean isAdjustedEmissionsAdded;
    private String emitterCode;
    private Boolean isSubjectToPeriodicReport;
    private Boolean isPeriodicReportTypeOne;
    private String energyReport;
    private String energyBusinessCode;
    private String factoryCode;
    
    private String repName;
    private String repRole;
    private String companyName;
    private Boolean hasConsent;
    private Boolean isAgreeTerms;
    private String agreedAt;

    // GETTER dengan @DynamoDbAttribute

    @DynamoDbPartitionKey
    @DynamoDbAttribute("pk")
    public String getPk() { return pk; }

    @DynamoDbSortKey
    @DynamoDbAttribute("sk")
    public String getSk() { return sk; }

    @DynamoDbAttribute("project_id")
    public Integer getProjectId() { return projectId; }

    @DynamoDbAttribute("site_id")
    public Integer getSiteId() { return siteId; }

    @DynamoDbAttribute("company_id")
    public String getCompanyId() { return companyId; }

    @DynamoDbAttribute("participation_id")
    public Integer getParticipationId() { return participationId; }

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

    @DynamoDbAttribute("is_adjusted_emissions_added")
    public Boolean getIsAdjustedEmissionsAdded() { return isAdjustedEmissionsAdded; }

    @DynamoDbAttribute("emitter_code")
    public String getEmitterCode() { return emitterCode; }

    @DynamoDbAttribute("is_subject_to_periodic_report")
    public Boolean getIsSubjectToPeriodicReport() { return isSubjectToPeriodicReport; }

    @DynamoDbAttribute("is_subject_to_periodic_report")
    public Boolean getIsPeriodicReportTypeOne() { return isPeriodicReportTypeOne; }

    @DynamoDbAttribute("energy_report")
    public String getEnergyReport() { return energyReport; }

    @DynamoDbAttribute("energy_business_code")
    public String getEnergyBusinessCode() { return energyBusinessCode; }

    @DynamoDbAttribute("factory_code")
    public String getFactoryCode() { return factoryCode; }

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

    @DynamoDbAttribute("agreed_at")
    public String getAgreedAt() { return agreedAt; }
}