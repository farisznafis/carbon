package com.kct.jcredit.core_backend.service.documentdata;

import static com.kct.jcredit.core_backend.service.documentdata.DocumentValues.date;
import static com.kct.jcredit.core_backend.service.documentdata.DocumentValues.number;
import static com.kct.jcredit.core_backend.service.documentdata.DocumentValues.section;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.kct.jcredit.core_backend.constant.PermissionCode;
import com.kct.jcredit.core_backend.dto.response.ProjectDocumentDataResponse.Section;
import com.kct.jcredit.core_backend.entity.EnR002ReductionResultTotal;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.enums.ProjectFileType;
import com.kct.jcredit.core_backend.repository.postgres.EnR002ReductionResultTotalRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

/**
 * モニタリング報告書: who and what the project is, and what the batch has
 * calculated for it so far.
 *
 * <p>The batch figures are the project's running totals since it started, as
 * of the last batch run — not the figures of one reporting period. A batch
 * result adds to the one before it and carries no measured period, so the
 * quantities of a period (a second report onwards) have to be computed from
 * the monitoring records; that is not done here.
 */
@Component
@RequiredArgsConstructor
public class MonitoringReportDataProvider implements DocumentDataProvider {

    private static final String UNIT_ENERGY = "kWh";
    private static final String UNIT_ENERGY_LARGE = "MWh";
    private static final String UNIT_EMISSION = "t-CO2";
    private static final BigDecimal KWH_PER_MWH = BigDecimal.valueOf(1000);

    private final EnR002ReductionResultTotalRepository reductionResultTotalRepository;

    @Override
    public ProjectFileType fileType() {
        return ProjectFileType.MONITORING_REPORT;
    }

    /** The report is monitoring data, read under the same permission as the monitoring screens. */
    @Override
    public Optional<PermissionCode> additionalPermission() {
        return Optional.of(PermissionCode.MONITORING_READ);
    }

    @Override
    public List<Section> sections(Project project, String projectId, String username) {
        return List.of(
                ProjectRecordSections.overview(project),
                ProjectRecordSections.methodology(project),
                batchTotals(reductionResultTotalRepository.findByProjectId(project.getId()).orElse(null)),
                ProjectRecordSections.operator(project),
                ProjectRecordSections.personInCharge(project));
    }

    /**
     * The project's cumulative batch result. A project the batch has not run
     * for yet gets the section with empty values, so the reader sees there is
     * nothing to report rather than a missing section.
     *
     * @param total the project's total row, or {@code null} when there is none
     */
    private static Section batchTotals(EnR002ReductionResultTotal total) {
        boolean none = total == null;
        BigDecimal elpj = none ? null : total.getElpj();
        return section("batchTotals", "バッチ計算結果（プロジェクト開始からの累計）", List.of(
                date("batchProcessingDate", "最終バッチ処理日", none ? null : total.getProcessingDate()),
                number("elpj", "プロジェクト実施後電力量（ELPJ）", elpj, UNIT_ENERGY),
                // Stored in kWh; the forms that ask for MWh get it converted here.
                number("elpjMwh", "プロジェクト実施後電力量（ELPJ・MWh換算）",
                        elpj == null ? null : elpj.divide(KWH_PER_MWH), UNIT_ENERGY_LARGE),
                number("elbl", "ベースライン電力量（ELBL）", none ? null : total.getElbl(), UNIT_ENERGY),
                number("embl", "ベースライン排出量（EMBL）", none ? null : total.getEmbl(), UNIT_EMISSION),
                number("empj", "プロジェクト実施後排出量（EMPJ）", none ? null : total.getEmpj(), UNIT_EMISSION),
                number("er", "排出削減量（ER）", none ? null : total.getEr(), UNIT_EMISSION)));
    }
}
