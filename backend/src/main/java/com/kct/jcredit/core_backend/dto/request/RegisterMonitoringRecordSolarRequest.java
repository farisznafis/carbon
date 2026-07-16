package com.kct.jcredit.core_backend.dto.request;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kct.jcredit.core_backend.validation.DateFormat;
import com.kct.jcredit.core_backend.validation.Hankaku;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@NoArgsConstructor
@Data
public class RegisterMonitoringRecordSolarRequest {
    
   @NotNull(message = "プロジェクトIDを入力してください。")
    @JsonProperty("projectId")
    private Integer projectId;
    
    @NotNull(message = "拠点IDを入力してください。")
    @JsonProperty("siteId")
    private Integer siteId;
    
    @JsonProperty("userId")
    private Integer userId;
    
    @NotBlank(message = "プロジェクトタイプを入力してください。")
    @Hankaku
    @JsonProperty("projectType")
    private String projectType;
    
    @NotNull(message = "タイムスタンプを入力してください。")
    @DateFormat
    @JsonProperty("dateRecorded")
    private String dateRecorded; 
    
    // ELpv - 電力使用量
     @NotNull(message = "ELpv（電力使用量）を入力してください。")
     @DecimalMin(value = "0.0", message = "ELpvは0以上である必要があります。")
    @Digits(integer = 9, fraction = 3, message = "ELpvは整数部9桁、小数部3桁以内で入力してください。")
    @JsonProperty("ELpv_value")
    private BigDecimal elPv;
    
    // ELpvr - 電力供給量
    @NotNull(message = "ELpvr（電力供給量）を入力してください。")
    @DecimalMin(value = "0.0", message = "ELpvrは0以上である必要があります。")
    @Digits(integer = 9, fraction = 3, message = "ELpvrは整数部9桁、小数部3桁以内で入力してください。")
    @JsonProperty("ELpvr_value")
    private BigDecimal elPvr;
    
    @NotNull(message = "バッテリー交換フラグを入力してください。")
    @JsonProperty("changedBattery")
    private Boolean changedBattery;
    
    // ELPJBatteryValue - バッテリー放電 (1年1回又はバッテリー交換時のみ)
    @DecimalMin(value = "0.0", message = "ELPJバッテリー値は0以上である必要があります。")
    @Digits(integer = 9, fraction = 3, message = "ELPJバッテリー値は整数部9桁、小数部3桁以内で入力してください。")
    @JsonProperty("ELPJ_battery_value")
    private BigDecimal elpjBatteryValue;
    
    // ELPJControl - 制御値
    @NotNull(message = "ELPJ制御値を入力してください。")
    @DecimalMin(value = "0.0", message = "ELPJ制御値は0以上である必要があります。")
    @Digits(integer = 9, fraction = 3, message = "ELPJ制御値は整数部9桁、小数部3桁以内で入力してください。")
    @JsonProperty("ELPJ_control_value")
    private BigDecimal elpjControl;
    
    // ELPJAccuracy - 計測精度 (1回のみ登録)
    @DecimalMin(value = "0.0", message = "ELPJ計測精度は0以上である必要があります。")
    @DecimalMax(value = "100.0", message = "ELPJ計測精度は100以下である必要があります。")
    @Digits(integer = 3, fraction = 2, message = "ELPJ計測精度は整数部3桁、小数部2桁以内で入力してください。")
    @NotNull(message = "測定精度を入力してください。")
    @JsonProperty("ELPJ_accuracy")
    private BigDecimal elpjAccuracy;

     // Mandatory evidence files
    @NotBlank(message = "発電量（ELPV）の証憑ファイルをアップロードしてください。")
    @JsonProperty("ELpv_evidence")
    private String elPvEvidenceUrl;
    
    @NotBlank(message = "自家消費量（ELPVR）の証憑ファイルをアップロードしてください。")
    @JsonProperty("ELpvr_evidence")
    private String elPvrEvidenceUrl;
    
    @JsonProperty("ELPJ_battery_evidence")
    private String elpjBatterySpecificationUrl;
    
    @NotBlank(message = "制御機器稼働時間（ELPJ）の証憑ファイルをアップロードしてください。")
    @JsonProperty("ELPJ_control_equipment_spec_files")
    private String elpjControlEvidenceUrl;
}
