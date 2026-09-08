package com.kct.jcredit.core_backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.kct.jcredit.core_backend.dto.response.RowDetailActivityResponse.ComputedResult;
import com.kct.jcredit.core_backend.dto.response.RowDetailActivityResponse.EquipmentInfo;
import com.kct.jcredit.core_backend.dto.response.RowDetailActivityResponse.MonitoringInfo;
import com.kct.jcredit.core_backend.enums.PeriodStatus;

/**
 * Response untuk GET /kobetsu-katsudo/rows/{rowKey}.
 * Sub-record dibiarkan nested di sini karena semuanya cuma dipakai dalam konteks 1 row detail --
 * tidak dipakai ulang di response lain, jadi tidak perlu jadi file terpisah.
 */
public record RowDetailActivityResponse(
        String rowKey,
        PeriodStatus rowType,
        boolean isLive,
        SiteInfo site,
        EquipmentInfo equipment,
        MonitoringInfo monitoring,
        ComputedResult computedResult
) {

    public record SiteInfo(
            String siteId,
            String companyId,
            String companyName,
            String address,
            LocalDate membershipApplicationDate,
            BusinessOperatorFlags businessOperatorFlags
            // TODO: businessOperatorFlags (Carbon Neutral Plan / 省エネ法 / 温対法) belum disertakan --
            //   sumber tabelnya belum ketemu di skema yang sudah dikonfirmasi, lihat kontrak API
    ) {}

    public record PcsInfo(
            String manufacturer,
            String model,
        //     Integer quantity,
            String serialNumber,
            BigDecimal standbyPowerKw,
            String meterClass,          // TODO: 分類B/C -- tidak ada kolomnya di EN_R002_equipment
            BigDecimal errorRatePercent // TODO: 最大誤差率(%) -- tidak ada kolomnya di EN_R002_equipment
    ) {}

    public record BusinessOperatorFlags(
                Boolean carbonNeutralActionPlan,      // カーボンニュートラル行動計画
                Boolean energySavingActSpecifiedOperator,  // 省エネ法特定事業者
                Boolean warmingActSpecifiedEmitter,   // 温対法特定排出者
                String emitterCode                    // 温対法の特定排出者コード 
        ) {}

    public record BatteryInfo(
            String manufacturer,
            String model,
            BigDecimal capacityKwh,
            BigDecimal chargeEfficiency,    // TODO: tidak ada kolomnya
            BigDecimal dischargeEfficiency  // TODO: tidak ada kolomnya
    ) {}

    public record InvestmentInfo(
            Integer totalInvestment,
            boolean hasSubsidy,
            String subsidyName,
            BigDecimal subsidyAmount,
            String subsidyGrantingBody, // TODO: 補助金交付団体 -- tidak ada kolomnya
            BigDecimal preProjectUnitPrice
    ) {}

    public record FileRef(String fileType, String url) {}

    public record EquipmentInfo(
            String equipmentId,
            LocalDate effectiveFrom,
            String effectiveTo,
            LocalDate operationStartDate,
            PcsInfo pcs,
            BigDecimal nominalOutputKw,
            BatteryInfo battery,
            BigDecimal ictConsumptionKw, // TODO: tidak ada tabel/kolom untuk ICT機器 sama sekali
            InvestmentInfo investment,
            List<FileRef> files
    ) {}

    public record MonitoringPoint(LocalDateTime date, BigDecimal generationKwh, BigDecimal saleKwh) {}

    public record MonitoringInfo(
            String samplingStatus,    // TODO: tidak ada di monitoring_table_record
            String meteringPosition,  // TODO: tidak ada di monitoring_table_record
            String monitoringMethod,  // TODO: tidak ada di monitoring_table_record
            MonitoringPoint periodStart,
            MonitoringPoint periodEnd,
            List<FileRef> files
    ) {}

    public record CertificationPeriod(
            LocalDate start,
            LocalDate end,
            String endReason // PROJECT_DEFAULT | WITHDRAWAL | BASELINE_EXTENDED -- 2 terakhir belum diimplementasi
    ) {}

    public record EmissionBreakdown(BigDecimal empj, BigDecimal elpj, BigDecimal embl, BigDecimal elbl) {}

    public record EligibilityChecklist(
            Boolean isEligible,
            Boolean hasAdditionality,       // TODO: judgment manual, belum ada kolomnya
            Boolean noDuplicateRegistration // TODO: proses terpisah (二重登録確認用リスト), belum ada kolomnya
    ) {}

    public record ComputedResult(
            CertificationPeriod certificationPeriod,
            BigDecimal reductionTco2,
            EmissionBreakdown emissionBreakdown,
            EligibilityChecklist eligibilityChecklist
    ) {}
}