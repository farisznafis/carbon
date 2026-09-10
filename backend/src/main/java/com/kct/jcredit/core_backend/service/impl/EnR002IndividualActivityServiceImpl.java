package com.kct.jcredit.core_backend.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Optional;
 
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kct.jcredit.core_backend.enums.ReportReadiness;
import com.kct.jcredit.core_backend.enums.EligibilityStatus;
import com.kct.jcredit.core_backend.dto.response.EnR002IndividualActivityDetailResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002IndividualActivityRowResponse;
import com.kct.jcredit.core_backend.dto.response.CompanyActivityGroupResponse;

import com.kct.jcredit.core_backend.service.EnR002IndividualActivityService;

import com.kct.jcredit.core_backend.entity.BatchProcessHistory;
import com.kct.jcredit.core_backend.entity.EnR002Equipment;
import com.kct.jcredit.core_backend.entity.EnR002ReductionResult;
import com.kct.jcredit.core_backend.entity.EnR002ReductionResultTotal;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingProject;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.EnR002MonitoringRecord;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.repository.postgres.BatchProcessHistoryRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002EquipmentFileRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002EquipmentRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002ReductionResultRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002ReductionResultTotalRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingProjectRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.repository.dynamodb.EnR002ApplicationConfirmationRepository;
import com.kct.jcredit.core_backend.repository.dynamodb.MonitoringTableRecordSolarRepository;
import com.kct.jcredit.core_backend.service.ProjectService;
import com.kct.jcredit.core_backend.service.batch.calculator.SolarEmissionsCalculator;




@Service
@Transactional(readOnly = true)
public class EnR002IndividualActivityServiceImpl implements EnR002IndividualActivityService {
 
    private static final ZoneId ZONE = ZoneId.of("Asia/Tokyo");
 
    /** Address prefix check for the 日本国内で実施 requirement. */
    private static final String JP_PREFECTURE_SUFFIX_PATTERN = ".*[都道府県].*";
 
    private final ProjectRepository projectRepository;
    private final ParticipatingProjectRepository participatingProjectRepository;
    private final ParticipatingCompanyRepository participatingCompanyRepository;
    private final EnR002EquipmentRepository equipmentRepository;
    private final EnR002EquipmentFileRepository equipmentFileRepository;
    private final EnR002ReductionResultRepository reductionResultRepository;
    private final EnR002ApplicationConfirmationRepository applicationConfirmationRepository;
    private final ProjectService projectService;
 
    public EnR002IndividualActivityServiceImpl(
            ProjectRepository projectRepository,
            ParticipatingProjectRepository participatingProjectRepository,
            ParticipatingCompanyRepository participatingCompanyRepository,
            EnR002EquipmentRepository equipmentRepository,
            EnR002EquipmentFileRepository equipmentFileRepository,
            EnR002ReductionResultRepository reductionResultRepository,
            EnR002ApplicationConfirmationRepository applicationConfirmationRepository,
            ProjectService projectService) {
        this.projectRepository = projectRepository;
        this.participatingProjectRepository = participatingProjectRepository;
        this.participatingCompanyRepository = participatingCompanyRepository;
        this.equipmentRepository = equipmentRepository;
        this.equipmentFileRepository = equipmentFileRepository;
        this.reductionResultRepository = reductionResultRepository;
        this.applicationConfirmationRepository = applicationConfirmationRepository;
        this.projectService = projectService;
    }
 
    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------
 
    @Override
    public List<EnR002IndividualActivityRowResponse> getRowsByProject(
            String projectPublicId, String companyIdFilter, String currentUserId) {
 
        projectService.assertProject(projectPublicId, currentUserId);
        Project project = resolveProject(projectPublicId);
 
        List<ParticipatingProject> participations = companyIdFilter != null
                ? participatingProjectRepository.findByProject_IdAndEquipment_Site_Company_CompanyId(
                        project.getId(), companyIdFilter)
                : participatingProjectRepository.findByProject_Id(project.getId());
 
        return buildRows(project, participations);
    }
 
    @Override
    public List<CompanyActivityGroupResponse> getRowsByCompany(
            String companyId, String projectPublicIdFilter, String currentUserId) {
 
        ParticipatingCompany company = participatingCompanyRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));
 
        List<ParticipatingProject> participations =
                participatingProjectRepository.findByEquipment_Site_Company_Id(company.getId());
 
        // Silently drop projects outside the current user's scope, and apply the optional filter.
        Map<Project, List<ParticipatingProject>> byProject = participations.stream()
                .filter(p -> matchesProjectFilter(p, projectPublicIdFilter))
                .filter(p -> projectService.hasProjectAccess(p.getProject().getPublicId(), currentUserId))
                .collect(Collectors.groupingBy(
                        ParticipatingProject::getProject,
                        LinkedHashMap::new,
                        Collectors.toList()));
 
        return byProject.entrySet().stream()
                .map(entry -> new CompanyActivityGroupResponse(
                        entry.getKey().getPublicId(),
                        entry.getKey().getProjectNumber(),
                        buildRows(entry.getKey(), entry.getValue())))
                .toList();
    }
 
    @Override
    public EnR002IndividualActivityDetailResponse getRowDetail(Integer equipmentId, String currentUserId) {
        EnR002Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new IndividualActivityNotFoundException(equipmentId));
 
        ParticipatingProject participation = participatingProjectRepository
                .findByEquipment_Id(equipmentId).stream()
                .findFirst()
                .orElseThrow(() -> new IndividualActivityNotFoundException(equipmentId));
 
        Project project = participation.getProject();
        projectService.assertProject(project.getPublicId(), currentUserId);
 
        ReportingWindow window = loadReportingWindow(project.getId(), equipment);
 
        return new EnR002IndividualActivityDetailResponse(
                equipment.getId(),
                buildMemberInfo(equipment, participation),
                buildEquipmentInfo(equipment),
                buildAdditionalityInfo(equipment),
                buildRegistrationRequirements(equipment, participation),
                buildMonitoringInfo(window)
        );
    }
 
    // ------------------------------------------------------------------
    // Row assembly
    // ------------------------------------------------------------------
 
    /**
     * Builds every row of one project in a fixed number of queries: the reduction
     * results for the whole project are fetched once and grouped in memory, rather
     * than queried per equipment.
     */
    private List<EnR002IndividualActivityRowResponse> buildRows(
            Project project, List<ParticipatingProject> participations) {
 
        if (participations.isEmpty()) {
            return List.of();
        }
 
        Map<Integer, ReportingWindow> windowsByEquipment = loadReportingWindows(project.getId());
 
        return participations.stream()
                .map(participation -> toRow(participation, windowsByEquipment))
                .toList();
    }
 
    private EnR002IndividualActivityRowResponse toRow(
            ParticipatingProject participation, Map<Integer, ReportingWindow> windowsByEquipment) {
 
        EnR002Equipment equipment = participation.getEquipment();
        Site site = equipment.getSite();
        ParticipatingCompany company = site.getCompany();
        ReportingWindow window = windowsByEquipment.getOrDefault(equipment.getId(), ReportingWindow.empty());
 
        return new EnR002IndividualActivityRowResponse(
                equipment.getId(),
                company != null ? company.getCompanyId() : null,
                company != null ? company.getNameJa() : null,
                site.getPublicId(),
                formatAddress(site),
                Boolean.TRUE.equals(equipment.getHasBattery()),
                toEligibilityStatus(participation),
                resolveReadiness(equipment, window),
                null, // SOURCE MISSING: 事業者区分 — manual judgement // window.latest().map(EnR002ReductionResult::getElPv).orElse(null),
                null // SOURCE MISSING: 事業者コード — manual judgement // window.latest().map(EnR002ReductionResult::getElPvr).orElse(null) 
        );
    }
 
    // ------------------------------------------------------------------
    // Reporting window — the two monitoring column blocks of 別紙10-1
    // ------------------------------------------------------------------
 
    /**
     * Loads, for every equipment in a project, the pair of reduction results that fill
     * the 開始日 and 終了日 blocks: the last result already submitted to J-Credit and
     * the most recent result overall.
     */
    private Map<Integer, ReportingWindow> loadReportingWindows(Integer projectId) {
        return reductionResultRepository.findByProjectIdAndIsActiveTrue(projectId).stream()
                .collect(Collectors.groupingBy(
                        EnR002ReductionResult::getEquipmentId,
                        Collectors.collectingAndThen(Collectors.toList(), ReportingWindow::from)));
    }
 
    private ReportingWindow loadReportingWindow(Integer projectId, EnR002Equipment equipment) {
        return ReportingWindow.from(
                reductionResultRepository.findByProjectIdAndEquipmentIdAndIsActiveTrue(
                        projectId, equipment.getId()));
    }
 
    /**
     * The 開始日 / 終了日 pair for one equipment.
     *
     * @param lastReported the newest result already submitted to J-Credit; empty for a
     *                     first-time report, in which case the 開始日 block is reported as zero
     * @param latest       the newest result of any kind, filling the 終了日 block
     */
    private record ReportingWindow(
            Optional<EnR002ReductionResult> lastReported,
            Optional<EnR002ReductionResult> latest) {
 
        private static final Comparator<EnR002ReductionResult> BY_BATCH =
                Comparator.comparing(EnR002ReductionResult::getBatchId,
                        Comparator.nullsFirst(Comparator.naturalOrder()));
 
        static ReportingWindow from(List<EnR002ReductionResult> results) {
            // SOURCE PENDING: `isReported` marks the batch result that was actually submitted
            // to J-Credit. The column is not in the schema yet — until it is added, every row
            // is treated as a first-time report and the 開始日 block stays zero.
            Optional<EnR002ReductionResult> lastReported = results.stream()
                    .filter(null) //.filter(r -> Boolean.TRUE.equals(r.getIsReported()))
                    .max(BY_BATCH);
 
            return new ReportingWindow(lastReported, results.stream().max(BY_BATCH));
        }
 
        static ReportingWindow empty() {
            return new ReportingWindow(Optional.empty(), Optional.empty());
        }
    }
 
    // ------------------------------------------------------------------
    // Detail section builders
    // ------------------------------------------------------------------
 
    private EnR002IndividualActivityDetailResponse.MemberInfo buildMemberInfo(
            EnR002Equipment equipment, ParticipatingProject participation) {
 
        Site site = equipment.getSite();
        ParticipatingCompany company = site.getCompany();
 
        return new EnR002IndividualActivityDetailResponse.MemberInfo(
                company != null ? company.getCompanyId() : null,
                company != null ? company.getNameJa() : null,
                site.getPublicId(),
                formatAddress(site),
                // The 入会届 has no explicit application-date field, so the record's creation
                // timestamp stands in for it. Confirm this is acceptable for reporting.
                participation.getCreatedAt().toLocalDate(),
                toEligibilityStatus(participation),
                loadBusinessOperatorFlags(participation)
        );
    }
 
    private EnR002IndividualActivityDetailResponse.BusinessOperatorFlags loadBusinessOperatorFlags(
            ParticipatingProject participation) {
 
        return applicationConfirmationRepository.findByParticipationId(participation.getId())
                .map(confirmation -> new EnR002IndividualActivityDetailResponse.BusinessOperatorFlags(
                        confirmation.getIsKeidanrenParticipation(),
                        // AMBIGUOUS: 省エネ法特定事業者 may map to isPeriodicReportTypeOne or to
                        // isSubjectToPeriodicReport. Confirm with whoever designed the 入会届 form.
                        confirmation.getIsPeriodicReportTypeOne(),
                        confirmation.getIsEmitter(),
                        confirmation.getEmitterCode()))
                .orElse(null);
    }
 
    private EnR002IndividualActivityDetailResponse.EquipmentInfo buildEquipmentInfo(EnR002Equipment equipment) {
        List<EnR002IndividualActivityDetailResponse.FileRef> files =
                equipmentFileRepository.findByEquipmentId(equipment.getId()).stream()
                        .map(file -> new EnR002IndividualActivityDetailResponse.FileRef(
                                file.getFileType().name(), file.getS3Key()))
                        .toList();
 
        return new EnR002IndividualActivityDetailResponse.EquipmentInfo(
                new EnR002IndividualActivityDetailResponse.PcsInfo(
                        equipment.getPcsManufacturer(),
                        equipment.getPcsModel(),
                        // equipment.getPcsUnitCount(),
                        equipment.getDeviceNo(),
                        equipment.getPcsStandby(),
                        null, // SOURCE MISSING: 分類B/C
                        null  // SOURCE MISSING: 最大誤差率(%)
                ),
                equipment.getNominalMaxOutput(),
                buildBatteryInfo(equipment),
                null, // SOURCE MISSING: ICT機器 消費電力(kW)
                equipment.getEquipmentOpDate(),
                equipment.getChangeDate() != null ? equipment.getChangeDate() : equipment.getEquipmentOpDate(),
                resolveEffectiveTo(equipment),
                files
        );
    }
 
    private EnR002IndividualActivityDetailResponse.BatteryInfo buildBatteryInfo(EnR002Equipment equipment) {
        if (!Boolean.TRUE.equals(equipment.getHasBattery())) {
            return null;
        }
        return new EnR002IndividualActivityDetailResponse.BatteryInfo(
                equipment.getBatteryMaker(),
                equipment.getBatteryModel(),
                equipment.getBatteryCapacity(),
                null, // SOURCE MISSING: 充電効率(%)
                null  // SOURCE MISSING: 放電効率(%)
        );
    }
 
    private EnR002IndividualActivityDetailResponse.AdditionalityInfo buildAdditionalityInfo(EnR002Equipment equipment) {
        return new EnR002IndividualActivityDetailResponse.AdditionalityInfo(
                equipment.getTotalInvestment(),
                equipment.getSubsidyAmount(),
                equipment.getSubsidyName(),
                null, // SOURCE MISSING: 補助金交付団体
                equipment.getPreProjectUnitPrice()
        );
    }
 
    private EnR002IndividualActivityDetailResponse.RegistrationRequirements buildRegistrationRequirements(
            EnR002Equipment equipment, ParticipatingProject participation) {
 
        LocalDate applicationDate = participation.getCreatedAt().toLocalDate();
        LocalDate operationDate = equipment.getEquipmentOpDate();
 
        return new EnR002IndividualActivityDetailResponse.RegistrationRequirements(
                isJapaneseAddress(equipment.getSite()),
                isWithinTwoYears(applicationDate, operationDate),
                null, // SOURCE MISSING: 追加性を有する — manual judgement
                null  // SOURCE MISSING: 削減活動の二重登録が無い — handled outside this system
        );
    }
 
    private EnR002IndividualActivityDetailResponse.MonitoringInfo buildMonitoringInfo(ReportingWindow window) {
        return new EnR002IndividualActivityDetailResponse.MonitoringInfo(
                null, // SOURCE MISSING: サンプリング
                null, // SOURCE MISSING: 発電電力量のモニタリング位置 (A/B)
                null, // SOURCE MISSING: モニタリング方法 (発電量)
                null, // SOURCE MISSING: モニタリング方法 (売電量)
                toMonitoringPoint(window.lastReported()),
                toMonitoringPoint(window.latest()),
                // Monitoring evidence lives on the DynamoDB records rather than on the batch
                // result, so it is not resolved here. Add a lookup once the UI needs the files.
                List.of()
        );
    }
 
    /**
     * Maps a batch result to one monitoring column block. An empty result means the member
     * has never been reported before, which 別紙10-1 expects to be filled with zeros.
     */
    private EnR002IndividualActivityDetailResponse.MonitoringPoint toMonitoringPoint(
            Optional<EnR002ReductionResult> result) {
 
        return result
                .map(r -> new EnR002IndividualActivityDetailResponse.MonitoringPoint(
                        r.getProcessingDate(),
                        null, // SOURCE MISSING: 検針日 — only one date column exists on the record
                        null, //r.getElPv(),
                        null)) //r.getElPvr()))
                .orElseGet(() -> new EnR002IndividualActivityDetailResponse.MonitoringPoint(
                        null, null, BigDecimal.ZERO, BigDecimal.ZERO));
    }
 
    // ------------------------------------------------------------------
    // Small helpers
    // ------------------------------------------------------------------
 
    private Project resolveProject(String publicId) {
        return projectRepository.findByPublicId(publicId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + publicId));
    }
 
    private boolean matchesProjectFilter(ParticipatingProject participation, String projectPublicIdFilter) {
        return projectPublicIdFilter == null
                || projectPublicIdFilter.equals(participation.getProject().getPublicId());
    }
 
    private EligibilityStatus toEligibilityStatus(ParticipatingProject participation) {
        if (participation == null || participation.getIsEligible() == null) {
            return EligibilityStatus.PENDING;
        }
        return participation.getIsEligible() ? EligibilityStatus.ELIGIBLE : EligibilityStatus.NOT_ELIGIBLE;
    }
 
    private ReportReadiness resolveReadiness(EnR002Equipment equipment, ReportingWindow window) {
        if (window.latest().isEmpty()) {
            return ReportReadiness.NO_BATCH_RESULT;
        }
        // Readiness currently means "at least one evidence file exists". Tighten this once the
        // required file types per equipment configuration are confirmed.
        boolean hasEvidence = equipmentFileRepository.countByEquipmentId(equipment.getId()) > 0;
        return hasEvidence ? ReportReadiness.READY : ReportReadiness.MISSING_EVIDENCE;
    }
 
    private LocalDate resolveEffectiveTo(EnR002Equipment equipment) {
        // The successor version's change date is where this version stops being valid.
        return equipmentRepository.findByPreviousEquipmentId(equipment.getId())
                .map(EnR002Equipment::getChangeDate)
                .orElse(null);
    }
 
    private boolean isJapaneseAddress(Site site) {
        String prefecture = site.getPrefecture();
        return prefecture != null && prefecture.matches(JP_PREFECTURE_SUFFIX_PATTERN);
    }
 
    private boolean isWithinTwoYears(LocalDate applicationDate, LocalDate operationDate) {
        if (applicationDate == null || operationDate == null) {
            return false;
        }
        return !operationDate.isBefore(applicationDate.minusYears(2));
    }
 
    private String formatAddress(Site site) {
        return java.util.stream.Stream.of(
                        site.getPrefecture(), site.getMunicipality(),
                        site.getBlocknum(), site.getBuildingName())
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" "));
    }
 
    /** Thrown when an equipment id does not resolve to a reportable row. */
    public static class IndividualActivityNotFoundException extends RuntimeException {
        public IndividualActivityNotFoundException(Integer equipmentId) {
            super("No individual activity row found for equipment: " + equipmentId);
        }
    }
}