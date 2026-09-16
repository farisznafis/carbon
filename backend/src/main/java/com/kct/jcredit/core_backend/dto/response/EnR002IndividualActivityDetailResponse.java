package com.kct.jcredit.core_backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.kct.jcredit.core_backend.enums.EligibilityStatus;


public record EnR002IndividualActivityDetailResponse(
        Integer equipmentId,
        MemberInfo member,
        EquipmentInfo equipment,
        AdditionalityInfo additionality,
        RegistrationRequirements registrationRequirements,
        MonitoringInfo monitoring
) {
 
/** 会員基本情報 — collected during site registration and 入会届. */
public record MemberInfo(
        String companyId,
        String companyName,
        String siteId,
        /** 設置場所住所 — must start with the prefecture name. */
        String address,
        /** 入会申込日. Currently derived from the participation record's creation timestamp. */
        LocalDate membershipApplicationDate,
        EligibilityStatus eligibilityStatus,
        /** Only applicable when the member is a business operator (事業者). */
        BusinessOperatorFlags businessOperatorFlags
) {}
 
/** The three 事業者のみ checkboxes, sourced from the 入会届 confirmation record. */
public record BusinessOperatorFlags(
        /** カーボンニュートラル行動計画への参加有無. */
        Boolean carbonNeutralActionPlan,
        /** 省エネ法特定事業者の該当有無. */
        Boolean energySavingActSpecifiedOperator,
        /** 温対法特定排出者の該当有無. */
        Boolean warmingActSpecifiedEmitter,
        /** Emitter code recorded alongside the 温対法 flag, when applicable. */
        String emitterCode
) {}
 
    /** 設備情報 — collected during equipment registration (設備登録). */
public record EquipmentInfo(
        PcsInfo pcs,
        /** 公称最大出力 (kW). */
        BigDecimal nominalOutputKw,
        /** Null when the equipment has no battery. */
        BatteryInfo battery,
        /** ICT機器 消費電力 (kW). SOURCE MISSING: no table or column exists for ICT devices. */
        BigDecimal ictConsumptionKw,
        /** 設備稼働日 (発電開始日) — the anchor date for all 原単位 calculations. */
        LocalDate operationStartDate,
        /** Equipment version validity start, from the change history chain. */
        LocalDate effectiveFrom,
        /** Null while this is the currently active version. */
        LocalDate effectiveTo,
        List<FileRef> evidenceFiles
) {}
 
public record PcsInfo(
        String manufacturer,
        String model,
        // Integer unitCount,
        // /** 機器固有番号 — must match the label or warranty document exactly. */
        String serialNumber,
        /** パワーコンディショナー夜間待機電力 (kW). */
        BigDecimal standbyPowerKw,
        /** 分類B or 分類C. SOURCE MISSING: not stored in EN_R002_equipment. */
        String meterClass,
        /** 最大誤差率 (%), only required for 分類C. SOURCE MISSING: not stored. */
        BigDecimal maxErrorRatePercent
) {}

public record BatteryInfo(
        String manufacturer,
        String model,
        /** 蓄電池容量 (kWh). */
        BigDecimal capacityKwh,
        /** 充電効率 (%). SOURCE MISSING: not stored in EN_R002_equipment. */
        BigDecimal chargeEfficiencyPercent,
        /** 放電効率 (%). SOURCE MISSING: not stored in EN_R002_equipment. */
        BigDecimal dischargeEfficiencyPercent
) {}
 
/** 追加性評価 — investment figures used to justify additionality. */
public record AdditionalityInfo(
        /** 総投資額 (円). */
        Integer totalInvestment,
        /** 補助金額 (円). */
        BigDecimal subsidyAmount,
        /** 補助金名称. */
        String subsidyName,
        /** 補助金交付団体. SOURCE MISSING: not stored in EN_R002_equipment. */
        String subsidyGrantingBody,
        /** 導入前1年間の平均電力単価 (円/kWh). */
        BigDecimal preProjectUnitPrice
) {}

/**
 * プロジェクト登録要件 — the ○/× checklist.
 * Values that the system can derive are computed; the rest are manual judgements
 * with no storage location yet and are returned as null.
 */
public record RegistrationRequirements(
        /** 日本国内で実施 — derived from the site address. */
        Boolean operatingInJapan,
        /** 入会申し込み日の2年前の日以降に実施 — derived from the two dates. */
        Boolean withinTwoYearsOfApplication,
        /** 追加性を有する. SOURCE MISSING: manual judgement, no column. */
        Boolean hasAdditionality,
        /** 削減活動の二重登録が無い. SOURCE MISSING: tracked separately via 二重登録確認用リスト. */
        Boolean noDuplicateRegistration
) {}
 
/**
 * モニタリング — the two column blocks of 別紙10-1.
 * <p>
 * {@code windowStart} carries forward the values submitted in the previous report
 * (zero for a first-time report); {@code windowEnd} holds the latest cumulative
 * batch result. Both are cumulative totals, not per-period deltas.
 */
public record MonitoringInfo(
        /** サンプリング. SOURCE MISSING: not stored on the monitoring record. */
        String samplingStatus,
        /** 発電電力量のモニタリング位置 (A or B). SOURCE MISSING: not stored. */
        String meteringPosition,
        /** モニタリング方法 for 発電量. SOURCE MISSING: not stored. */
        String generationMethod,
        /** モニタリング方法 for 売電量. SOURCE MISSING: not stored. */
        String saleMethod,
        /** 今回認証のモニタリング開始日 block. */
        MonitoringPoint windowStart,
        /** 今回認証のモニタリング終了日 block. */
        MonitoringPoint windowEnd,
        List<FileRef> evidenceFiles
) {}
 
/** One of the two repeated four-column blocks in the monitoring section. */
public record MonitoringPoint(
        /** 積算値データの取得日. */
        LocalDate readingDate,
        /** 電力会社発行の明細に記載された検針日. SOURCE MISSING: only one date column exists. */
        LocalDate utilityMeterReadingDate,
        /** 発電量 (kWh), cumulative. */
        BigDecimal generationKwh,
        /** 売電量 (kWh), cumulative. */
        BigDecimal saleKwh
) {}
 
public record FileRef(String fileType, String url) {}
}
 