package com.kct.jcredit.core_backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnR002EquipmentRequest {
 
    @NotBlank
    private String tempId;
     
    @NotNull
    private String siteId;
    // ── 設備稼働日 ──────────────────────────────────────────────
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate equipmentOpDate;

    // ── 太陽光パネルの情報 ────────────────────────────────────────
    // 定格最大出力 (kW) 
    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal nominalMaxOutput;

    // ── パワーコンディショナーの情報 ──────────────────────────────
    @NotBlank
    private String pcsManufacturer;

    @NotBlank
    private String pcsModel;

    // // 台数 — minimal 1
    // @NotNull
    // @Min(1)
    // private Integer pcsUnitCount;

    // 機器固有番号（製造番号）
    @NotBlank
    private String deviceNo;

    // 夜間待機電力（W）— konfirmasi satuan W dari UI design
    @NotNull
    @DecimalMin("0.0")
    private BigDecimal pcsStandby;

    // ── 蓄電池の情報 ──────────────────────────────────────────────
    @NotNull
    private Boolean hasBattery;

    // conditional: wajib kalau hasBattery = true
    private String batteryMaker;
    private String batteryModel;

    // 蓄電池容量 (kWh) — confirmed dari dokumen monitoring
    private BigDecimal batteryCapacity;

    // // ── ICT機器 (任意) ────────────────────────────────────────────
    // // HEMS等、クラウドに情報を送信する機器を使用している場合
    // @DecimalMin("0.0")
    // private BigDecimal ictEquipmentPowerConsumption; //ini total jadi ga perlu

    // ── 追加性評価に必要な情報 ────────────────────────────────────
    @NotNull
    @Min(1)
    private Integer totalInvestment;

    @NotNull
    private Boolean hasSubsidy;

    // conditional: wajib kalau hasSubsidy = true
    private String subsidyName;
    private BigDecimal subsidyAmount;

    // プロジェクト実施前の電力購入単価 (円/kWh)
    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal preProjectUnitPrice;

    // // ── 追加設備 ──────────────────────────────────────────────────
    // @NotNull additional harusnya pas nambah unit 
    // private Boolean additionalEquipment;

    // // conditional: wajib kalau additionalEquipment = true
    // @JsonFormat(pattern = "yyyy-MM-dd")
    // private LocalDate additionalEquipmentDate;

    // private String equipmentType;

    // @Min(1)
    // private Integer unitCount;

    // private String deviceUniqueNo;

    // ── ファイル証憑 ──────────────────────────────────────────────
 
    @NotEmpty  @Valid
    private List<EnR002FileItemRequest> files;
}