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
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ProjectParticipationConfirmation {

    private String pk; // "PART#{participationId}"

    // ── 添付資料確認欄（7項目）─────────────────────────────
    private Boolean isSolarContractDoc;
    private Boolean isPcsSpecDoc;
    private Boolean isBatterySpecDoc;
    private Boolean isOperationStartDoc;
    private Boolean isGenerationDataDoc;
    private Boolean isReversePowerDoc;
    private Boolean isInvestmentProofDoc;

    // ── データ管理確認欄（5項目）───────────────────────────
    private Boolean isApplicationCompleteCheck;
    private Boolean isFormsAttachCheck;
    private Boolean isEmissionsReductionActivityListCheck;
    private Boolean isOriginalSpecCheck;
    private Boolean isSubsidyCheck;

    // ── 確認者記名欄（公式スキーマ）─────────────────────────
    private String primaryApplReviewerName;
    private String primaryApplReviewerInkan;
    private String secondaryApplReviewerName;
    private String secondaryApplReviewerInkan;

    // ── 内部管理用フィールド（公式スキーマ外）────────────────
    // 「同一人物が一次・二次を兼任できない」制御のために追加。
    // PDF出力には使わない、バックエンドの整合性チェック専用。
    private Integer primaryReviewerId;
    private Integer secondaryReviewerId;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("pk")
    public String getPk() { return pk; }

    @DynamoDbAttribute("is_solar_contract_doc")
    public Boolean getIsSolarContractDoc() { return isSolarContractDoc; }

    @DynamoDbAttribute("is_pcs_spec_doc")
    public Boolean getIsPcsSpecDoc() { return isPcsSpecDoc; }

    @DynamoDbAttribute("is_battery_spec_doc")
    public Boolean getIsBatterySpecDoc() { return isBatterySpecDoc; }

    @DynamoDbAttribute("is_operation_start_doc")
    public Boolean getIsOperationStartDoc() { return isOperationStartDoc; }

    @DynamoDbAttribute("is_generation_data_doc")
    public Boolean getIsGenerationDataDoc() { return isGenerationDataDoc; }

    @DynamoDbAttribute("is_reverse_power_doc")
    public Boolean getIsReversePowerDoc() { return isReversePowerDoc; }

    @DynamoDbAttribute("is_investment_proof_doc")
    public Boolean getIsInvestmentProofDoc() { return isInvestmentProofDoc; }

    @DynamoDbAttribute("is_application_complete_check")
    public Boolean getIsApplicationCompleteCheck() { return isApplicationCompleteCheck; }

    @DynamoDbAttribute("is_forms_attach_check")
    public Boolean getIsFormsAttachCheck() { return isFormsAttachCheck; }

    @DynamoDbAttribute("is_emissions_reduction_activity_list_check")
    public Boolean getIsEmissionsReductionActivityListCheck() { return isEmissionsReductionActivityListCheck; }

    @DynamoDbAttribute("is_original_spec_check")
    public Boolean getIsOriginalSpecCheck() { return isOriginalSpecCheck; }

    @DynamoDbAttribute("is_subsidy_check")
    public Boolean getIsSubsidyCheck() { return isSubsidyCheck; }

    @DynamoDbAttribute("primary_appl_reviewer_name")
    public String getPrimaryApplReviewerName() { return primaryApplReviewerName; }

    @DynamoDbAttribute("primary_appl_reviewer_inkan")
    public String getPrimaryApplReviewerInkan() { return primaryApplReviewerInkan; }

    @DynamoDbAttribute("secondary_appl_reviewer_name")
    public String getSecondaryApplReviewerName() { return secondaryApplReviewerName; }

    @DynamoDbAttribute("secondary_appl_reviewer_inkan")
    public String getSecondaryApplReviewerInkan() { return secondaryApplReviewerInkan; }

    @DynamoDbAttribute("primary_reviewer_id")
    public Integer getPrimaryReviewerId() { return primaryReviewerId; }

    @DynamoDbAttribute("secondary_reviewer_id")
    public Integer getSecondaryReviewerId() { return secondaryReviewerId; }
}