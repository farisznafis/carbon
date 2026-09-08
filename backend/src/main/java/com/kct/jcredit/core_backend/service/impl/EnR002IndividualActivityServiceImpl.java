package com.kct.jcredit.core_backend.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
 
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.kct.jcredit.core_backend.enums.CompletenessStatus;
import com.kct.jcredit.core_backend.enums.EligibilityStatus;
import com.kct.jcredit.core_backend.enums.PeriodStatus;
import com.kct.jcredit.core_backend.dto.response.RowDetailActivityResponse;
import com.kct.jcredit.core_backend.dto.response.RowSummaryActivityResponse;
import com.kct.jcredit.core_backend.dto.response.PeriodSummaryResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectGroupResponse;

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
public class EnR002IndividualActivityServiceImpl implements EnR002IndividualActivityService {
 
    private static final ZoneId ZONE = ZoneId.of("Asia/Tokyo"); 
 
    private final ProjectRepository projectRepository;
    private final ParticipatingProjectRepository participatingProjectRepository;
    private final ParticipatingCompanyRepository participatingCompanyRepository;
    private final EnR002EquipmentRepository equipmentRepository;
    private final EnR002EquipmentFileRepository equipmentFileRepository;
    private final EnR002ReductionResultRepository reductionResultRepository;
    private final EnR002ReductionResultTotalRepository reductionResultTotalRepository;
    private final BatchProcessHistoryRepository batchProcessHistoryRepository;
    private final MonitoringTableRecordSolarRepository monitoringTableRecordSolarRepository;
    private final ProjectService projectService;
    private final SolarEmissionsCalculator solarEmissionsCalculator;
    private final EnR002ApplicationConfirmationRepository applicationConfirmationRepository;
 
    public EnR002IndividualActivityServiceImpl(
            ProjectRepository projectRepository,
            ParticipatingProjectRepository participatingProjectRepository,
            ParticipatingCompanyRepository participatingCompanyRepository,
            EnR002EquipmentRepository equipmentRepository,
            EnR002EquipmentFileRepository equipmentFileRepository,
            EnR002ReductionResultRepository reductionResultRepository,
            EnR002ReductionResultTotalRepository reductionResultTotalRepository,
            BatchProcessHistoryRepository batchProcessHistoryRepository,
            MonitoringTableRecordSolarRepository monitoringTableRecordSolarRepository,
            ProjectService projectService,
            SolarEmissionsCalculator solarEmissionsCalculator,
            EnR002ApplicationConfirmationRepository applicationConfirmationRepository) {
        this.projectRepository = projectRepository;
        this.participatingProjectRepository = participatingProjectRepository;
        this.participatingCompanyRepository = participatingCompanyRepository;
        this.equipmentRepository = equipmentRepository;
        this.equipmentFileRepository = equipmentFileRepository;
        this.reductionResultRepository = reductionResultRepository;
        this.reductionResultTotalRepository = reductionResultTotalRepository;
        this.batchProcessHistoryRepository = batchProcessHistoryRepository;
        this.monitoringTableRecordSolarRepository = monitoringTableRecordSolarRepository;
        this.projectService = projectService;
        this.solarEmissionsCalculator = solarEmissionsCalculator;
        this.applicationConfirmationRepository = applicationConfirmationRepository;
    }
 
    // ============================================================
    // 1. periods
    // ============================================================
 
    @Override
    public List<PeriodSummaryResponse> getPeriods(String projectPublicId, String currentUserId) {
        projectService.assertProject(projectPublicId, currentUserId);
        Project project = resolveProject(projectPublicId);
        Integer projectId = project.getId();
 
        List<EnR002ReductionResultTotal> closedTotals =
                reductionResultTotalRepository.findByProjectIdOrderByEndTimeAsc(projectId);
 
        List<PeriodSummaryResponse> result = new ArrayList<>();
        for (EnR002ReductionResultTotal total : closedTotals) {
            BatchProcessHistory job = batchProcessHistoryRepository.findById(total.getBatchId()).orElse(null);
 
            long rowCount = reductionResultRepository.countByProjectIdAndBatchId(projectId, total.getBatchId());
 
            result.add(new PeriodSummaryResponse(
                    String.valueOf(total.getBatchId()),
                    PeriodStatus.CLOSED,
                    toLocalDate(total.getStartTime()),
                    toLocalDate(total.getEndTime()),
                    job != null ? toLocalDateTime(job.getEndTime()) : null,
                    total.getEr(),
                    rowCount
            ));
        }
 
        LocalDate runningStart = closedTotals.isEmpty()
                ? equipmentRepository.findEarliestOperationDateByProjectId(projectId)
                : toLocalDate(closedTotals.get(closedTotals.size() - 1).getEndTime()).plusDays(1);
 
        long runningRowCount = participatingProjectRepository.findByProject_Id(projectId).stream()
                .filter(this::isActiveParticipation)
                .count();
 
        if (runningRowCount > 0) {
            result.add(new PeriodSummaryResponse(
                    "current", PeriodStatus.RUNNING, runningStart, null, null, null, runningRowCount
            ));
        }
 
        return result;
    }
 
    // ============================================================
    // 2. rows 
    // ============================================================
 
    @Override
    public PageResponse<RowSummaryActivityResponse> getRowsForPeriod(
            String projectPublicId, String periodId, RowsQueryRequest request, String currentUserId) {
        projectService.assertProject(projectPublicId, currentUserId);
        Project project = resolveProject(projectPublicId);
        Integer projectId = project.getId();
        Pageable pageable = PageRequest.of(request.page(), request.size());
 
        if ("current".equals(periodId)) {
            List<ParticipatingProject> participations = request.companyId() != null
                    ? participatingProjectRepository.findByProject_IdAndEquipment_Site_Company_CompanyId(
                            projectId, request.companyId())
                    : participatingProjectRepository.findByProject_Id(projectId);
 
            List<RowSummaryActivityResponse> rows = participations.stream()
                    .filter(this::isActiveParticipation)
                    .map(this::buildRunningRowSummary)
                    .collect(Collectors.toList());
 
            return PageResponse.from(paginateInMemory(rows, pageable));
        }
 
        Integer batchId = Integer.valueOf(periodId); 
        Page<EnR002ReductionResult> resultsPage = request.companyId() != null
                ? reductionResultRepository.findByProjectIdAndBatchIdAndCompanyId(
                        projectId, batchId, request.companyId(), pageable)
                : reductionResultRepository.findByProjectIdAndBatchId(projectId, batchId, pageable);
 
        return PageResponse.from(resultsPage.map(this::buildClosedRowSummary));
    }
 
    // ============================================================
    // 3. rows per company
    // ============================================================
 
    @Override
    public List<ProjectGroupResponse> getRowsForCompany(
            String companyPublicId, CompanyKobetsuKatsudoRequest request, String currentUserId) {
        ParticipatingCompany company = participatingCompanyRepository.findByCompanyId(companyPublicId)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + companyPublicId));
 
        List<ParticipatingProject> participations = request.projectId() != null
                ? participatingProjectRepository.findByCompanyIdAndProject_Id(
                        company.getId(), resolveProject(request.projectId()).getId())
                        .map(List::of).orElse(List.of())
                : participatingProjectRepository.findByEquipment_Site_Company_Id(company.getId());
 
        Map<Integer, List<ParticipatingProject>> byProject = participations.stream()
                .filter(p -> projectService.hasProjectAccess(
                        String.valueOf(p.getProject().getId()), currentUserId))
                .collect(Collectors.groupingBy(p -> p.getProject().getId()));
 
        List<ProjectGroupResponse> groups = new ArrayList<>();
        for (Map.Entry<Integer, List<ParticipatingProject>> entry : byProject.entrySet()) {
            Project project = entry.getValue().get(0).getProject();
            List<PeriodSummaryResponse> periods = getPeriods(project.getPublicId(), currentUserId);
 
            List<ProjectGroupResponse.PeriodGroup> periodGroups = new ArrayList<>();
            for (PeriodSummaryResponse period : periods) {
                RowsQueryRequest rowsRequest = RowsQueryRequest.of(companyPublicId, 0, 1000);
                PageResponse<RowSummaryActivityResponse> rows =
                        getRowsForPeriod(project.getPublicId(), period.periodId(), rowsRequest, currentUserId);
 
                periodGroups.add(new ProjectGroupResponse.PeriodGroup(
                        period.periodId(), period.periodStatus(),
                        period.periodStartDate(), period.periodEndDate(),
                        rows.items()
                ));
            }
 
            groups.add(new ProjectGroupResponse(project.getPublicId(), resolveProjectName(project), periodGroups));
        }
        return groups;
    }
 
    // ============================================================
    // 4. detail 1 row
    // ============================================================
 
    @Override
    public RowDetailActivityResponse getRowDetail(String rowKey, String currentUserId) {
        DecodedRowKey decoded = decodeRowKey(rowKey);
 
        if (decoded.type() == PeriodStatus.CLOSED) {
            EnR002ReductionResult result = reductionResultRepository.findById(decoded.id())
                    .orElseThrow(() -> new RowNotFoundException(rowKey));
            projectService.assertProject(String.valueOf(result.getProjectId()), currentUserId);
            return buildClosedRowDetail(result);
        }
 
        ParticipatingProject participation = participatingProjectRepository.findById(decoded.id())
                .orElseThrow(() -> new RowNotFoundException(rowKey));
        projectService.assertProject(String.valueOf(participation.getProject().getId()), currentUserId);
        return buildRunningRowDetail(participation);
    }
 
    // ============================================================
    // Row builder -- CLOSED
    // ============================================================
 
    private RowSummaryActivityResponse buildClosedRowSummary(EnR002ReductionResult result) {
        EnR002Equipment equipment = equipmentRepository.findById(result.getEquipmentId())
                .orElseThrow(() -> new IllegalStateException("Equipment hilang untuk reduction_result " + result.getId()));
        Site site = equipment.getSite();
        Project project = projectRepository.findById(result.getProjectId())
        .orElseThrow(() -> new IllegalStateException("Project: " + result.getProjectId()));
        ParticipatingCompany company = site.getCompany();
        ParticipatingProject participation = participatingProjectRepository
                .findByEquipment_Site_IdAndProject_Id(site.getId(), project.getId())
                .orElse(null);
 
        return new RowSummaryActivityResponse(
                encodeRowKey(PeriodStatus.CLOSED, result.getId()),
                PeriodStatus.CLOSED,
                company != null ? company.getCompanyId() : null,
                company != null ? company.getNameJa() : null,
                site.getPublicId(),
                formatAddress(site),
                Boolean.TRUE.equals(equipment.getHasBattery()),
                mapEligibility(participation),
                computeCompletenessStatus(equipment, project, toLocalDateTime(result.getStartTime()), toLocalDateTime(result.getEndTime())),
                result.getEr()
        );
    }
 
    private RowDetailActivityResponse buildClosedRowDetail(EnR002ReductionResult result) {
        EnR002Equipment equipment = equipmentRepository.findById(result.getEquipmentId()).orElseThrow();
        Site site = equipment.getSite();
        Project project = projectRepository.findById(result.getProjectId())
                .orElseThrow(() -> new IllegalStateException("Project: " + result.getProjectId()));
        ParticipatingCompany company = site.getCompany();
        ParticipatingProject participation = participatingProjectRepository
                .findByEquipment_Site_IdAndProject_Id(site.getId(), project.getId())
                .orElse(null);
 
        LocalDateTime start = toLocalDateTime(result.getStartTime());
        LocalDateTime end = toLocalDateTime(result.getEndTime());
        List<EnR002MonitoringRecord> records = monitoringTableRecordSolarRepository
        .findByRecordIdAndEquipmentIdAndDateRecordedBetween(
                buildRecordId(site, project),
                equipment.getId(),          
                formatSk(start), formatSk(end));
 
        return new RowDetailActivityResponse(
                encodeRowKey(PeriodStatus.CLOSED, result.getId()),
                PeriodStatus.CLOSED,
                false,
                buildSiteInfo(site, company, participation),
                buildEquipmentInfo(equipment),
                buildMonitoringInfo(records),
                buildComputedResult(result, null)
        );
    }
 
    // ============================================================
    // Row builder -- RUNNING
    // ============================================================
 
    private RowSummaryActivityResponse buildRunningRowSummary(ParticipatingProject participation) {
        EnR002Equipment equipment = participation.getEquipment();
        Site site = equipment.getSite();
        Project project = participation.getProject();
        ParticipatingCompany company = site.getCompany();
 
        LocalDateTime periodStart = resolveRunningPeriodStart(project.getId(), equipment);
        List<EnR002MonitoringRecord> records = monitoringTableRecordSolarRepository
                .findByRecordIdAndEquipmentIdAndDateRecordedAfter(
                        buildRecordId(site, project),
                        equipment.getId(),
                        formatSk(periodStart));
 
        BigDecimal liveEr = null;
        if (!records.isEmpty()) {
            EnR002ReductionResult preview = computeLivePreview(
                    project.getId(), site.getId(), records);
            liveEr = preview != null ? preview.getEr() : null;
        }
 
        return new RowSummaryActivityResponse(
                encodeRowKey(PeriodStatus.RUNNING, participation.getId()),
                PeriodStatus.RUNNING,
                company != null ? company.getCompanyId() : null,
                company != null ? company.getNameJa() : null,
                site.getPublicId(),
                formatAddress(site),
                Boolean.TRUE.equals(equipment.getHasBattery()),
                mapEligibility(participation),
                computeCompletenessStatus(equipment, project, periodStart, null),
                liveEr
        );
    }
 
    private RowDetailActivityResponse buildRunningRowDetail(ParticipatingProject participation) {
        EnR002Equipment equipment = participation.getEquipment();
        Site site = equipment.getSite();
        Project project = participation.getProject();
        ParticipatingCompany company = site.getCompany();
 
        LocalDateTime periodStart = resolveRunningPeriodStart(project.getId(), equipment);
        List<EnR002MonitoringRecord> records = monitoringTableRecordSolarRepository
                .findByRecordIdAndEquipmentIdAndDateRecordedAfter(
                        buildRecordId(site, project),
                        equipment.getId(),
                        formatSk(periodStart));
 
        EnR002ReductionResult preview = computeLivePreview(
                project.getId(), site.getId(), records);
 
        return new RowDetailActivityResponse(
                encodeRowKey(PeriodStatus.RUNNING, participation.getId()),
                PeriodStatus.RUNNING,
                true,
                buildSiteInfo(site, company, participation),
                buildEquipmentInfo(equipment),
                buildMonitoringInfo(records),
                buildComputedResult(null, new LivePreview(periodStart, preview))
        );
    }
 
    private LocalDateTime resolveRunningPeriodStart(Integer projectId, EnR002Equipment equipment) {
        return reductionResultTotalRepository.findTopByProjectIdOrderByEndTimeDesc(projectId)
                .map(t -> toLocalDateTime(t.getEndTime()).plusDays(1))
                .orElse(equipment.getEquipmentOpDate().atStartOfDay());
    }
 
    private String buildRecordId(Site site, Project project) {
        return site.getPublicId() + "#" + project.getId();
        }
 
    // formatSk: 
    private String formatSk(LocalDateTime dateTime) {
        return dateTime.toString();
    }
 
    private EnR002ReductionResult computeLivePreview(
            Integer projectId, Integer siteId, List<EnR002MonitoringRecord> records) {
        if (records.isEmpty()) {
            return null;
        }
        try {
            List<EnR002ReductionResult> results = solarEmissionsCalculator.calculateEmissions(
                    projectId, siteId, null, records, LocalDate.now(ZONE));
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            // TODO: fail logic
            return null;
        }
    }
 
    // ============================================================
    // Sub-builder
    // ============================================================
 
    private RowDetailActivityResponse.SiteInfo buildSiteInfo(
                Site site, ParticipatingCompany company, ParticipatingProject participation) {

        RowDetailActivityResponse.BusinessOperatorFlags flags = null;
        if (participation != null) {
                flags = applicationConfirmationRepository.findByParticipationId(participation.getId())
                        .map(c -> new RowDetailActivityResponse.BusinessOperatorFlags(
                                c.getIsKeidanrenParticipation(),
                                c.getIsPeriodicReportTypeOne(), 
                                c.getIsEmitter(),
                                c.getEmitterCode()))
                        .orElse(null);
        }

        return new RowDetailActivityResponse.SiteInfo(
                site.getPublicId(),
                company != null ? company.getCompanyId() : null,
                company != null ? company.getNameJa() : null,
                formatAddress(site),
                participation != null ? participation.getCreatedAt().toLocalDate() : null,
                flags
        );
        }
 
    private RowDetailActivityResponse.EquipmentInfo buildEquipmentInfo(EnR002Equipment equipment) {
        String effectiveTo = equipmentRepository.findByPreviousEquipmentId(equipment.getId())
                .map(next -> next.getChangeDate().toString())
                .orElse(null);
 
        RowDetailActivityResponse.BatteryInfo battery = Boolean.TRUE.equals(equipment.getHasBattery())
                ? new RowDetailActivityResponse.BatteryInfo(
                        equipment.getBatteryMaker(),
                        equipment.getBatteryModel(),
                        equipment.getBatteryCapacity(),
                        null, // TODO: 充電効率(%) -- no coloumn
                        null  // TODO: 放電効率(%) -- no  coloumn
                  )
                : null;
 
        return new RowDetailActivityResponse.EquipmentInfo(
                String.valueOf(equipment.getId()),
                equipment.getChangeDate() != null ? equipment.getChangeDate() : equipment.getEquipmentOpDate(),
                effectiveTo,
                equipment.getEquipmentOpDate(),
                new RowDetailActivityResponse.PcsInfo(
                        equipment.getPcsManufacturer(),
                        equipment.getPcsModel(),
                        // equipment.getPcsUnitCount(),
                        equipment.getDeviceNo(),
                        equipment.getPcsStandby(),
                        null, // TODO: 分類B/C (meterClass) -- no coloumn
                        null  // TODO: 最大誤差率(%) -- no oloumn
                ),
                equipment.getNominalMaxOutput(),
                battery,
                null, // TODO: ICT機器 消費電力(kW) -- no coloumn
                new RowDetailActivityResponse.InvestmentInfo(
                        equipment.getTotalInvestment(),
                        Boolean.TRUE.equals(equipment.getHasSubsidy()),
                        equipment.getSubsidyName(),
                        equipment.getSubsidyAmount(),
                        null, // TODO: 補助金交付団体 -- no coloumn
                        equipment.getPreProjectUnitPrice()
                ),
                equipmentFileRepository.findByEquipmentId(equipment.getId()).stream()
                        .map(f -> new RowDetailActivityResponse.FileRef(f.getFileType().name(), f.getS3Key()))
                        .collect(Collectors.toList())
        );
    }
 
    private RowDetailActivityResponse.MonitoringInfo buildMonitoringInfo(List<EnR002MonitoringRecord> records) {
        List<EnR002MonitoringRecord> sorted = records.stream()
        .filter(r -> r.getDateRecorded() != null)
        .sorted(Comparator.comparing(EnR002MonitoringRecord::getDateRecorded))
        .toList();
        
        EnR002MonitoringRecord first = sorted.isEmpty() ? null : sorted.get(0);
        EnR002MonitoringRecord last = sorted.isEmpty() ? null : sorted.get(sorted.size() - 1);
 
        List<RowDetailActivityResponse.FileRef> files = records.stream()
                .flatMap(r -> List.of(
                        new RowDetailActivityResponse.FileRef("ELPV_EVIDENCE", r.getElPvEvidence()),
                        new RowDetailActivityResponse.FileRef("ELPVR_EVIDENCE", r.getElPvrEvidence())
                ).stream())
                .filter(f -> f.url() != null)
                .collect(Collectors.toList());
 
        return new RowDetailActivityResponse.MonitoringInfo(
                null, // TODO: samplingStatus -- no coloumn
                null, // TODO: meteringPosition (A/B) -- no coloumn
                null, // TODO: monitoring method -- no coloumn
                first != null ? new RowDetailActivityResponse.MonitoringPoint(
                        parseSk(first), first.getElPv(), first.getElPvr()) : null,
                last != null ? new RowDetailActivityResponse.MonitoringPoint(
                        parseSk(last), last.getElPv(), last.getElPvr()) : null,
                files
        );
    }
 
    private RowDetailActivityResponse.ComputedResult buildComputedResult(
            EnR002ReductionResult closedResult, LivePreview livePreview) {
 
        EnR002ReductionResult effective = closedResult != null
                ? closedResult
                : (livePreview != null ? livePreview.result() : null);
 
        LocalDate start = closedResult != null
                ? toLocalDate(closedResult.getStartTime())
                : (livePreview != null ? livePreview.periodStart().toLocalDate() : null);
        LocalDate end = closedResult != null ? toLocalDate(closedResult.getEndTime()) : null;
        String endReason = closedResult != null ? "PROJECT_DEFAULT" : null;
        // TODO: closedResult -- cant find endReason in DB, but API contract requires it. Confirm with API team.
 
        RowDetailActivityResponse.EmissionBreakdown breakdown = effective != null
                ? new RowDetailActivityResponse.EmissionBreakdown(
                        effective.getEmpj(), effective.getElpj(), effective.getEmbl(), effective.getElbl())
                : null;
 
        return new RowDetailActivityResponse.ComputedResult(
                new RowDetailActivityResponse.CertificationPeriod(start, end, endReason),
                effective != null ? effective.getEr() : null,
                breakdown,
                new RowDetailActivityResponse.EligibilityChecklist(
                        null, null, null // TODO: confirmation of eligibility checklist fields -- not found in DB
                )
        );
    }
 
    // ============================================================
    // Helper methods
    // ============================================================
 
    private Project resolveProject(String publicId) {
        return projectRepository.findByPublicId(publicId)
                .orElseThrow(() -> new IllegalArgumentException("Project tidak ditemukan: " + publicId));
    }
 
    private boolean isActiveParticipation(ParticipatingProject p) {
        // TODO: confrmation of what "active" means -- currently assuming status ACTIVE means active
        return "ACTIVE".equals(p.getStatus());
    }
 
    private LocalDate toLocalDate(Instant instant) {
        return instant == null ? null : instant.atZone(ZONE).toLocalDate();
    }
 
    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : instant.atZone(ZONE).toLocalDateTime();
    }
 
    private LocalDateTime parseSk(EnR002MonitoringRecord record) {
        return LocalDateTime.parse(record.getDateRecorded()); // TODO: samakan format parse dgn formatSk()
    }
 
    private CompletenessStatus computeCompletenessStatus(
        EnR002Equipment equipment, Project project, LocalDateTime start, LocalDateTime end) {
    boolean hasRequiredFiles = equipmentFileRepository.countByEquipmentId(equipment.getId()) > 0;
    boolean hasMonitoring = !(end == null
            ? monitoringTableRecordSolarRepository.findByRecordIdAndEquipmentIdAndDateRecordedAfter(
                    buildRecordId(equipment.getSite(), project),
                    equipment.getId(), formatSk(start))
            : monitoringTableRecordSolarRepository.findByRecordIdAndEquipmentIdAndDateRecordedBetween(
                    buildRecordId(equipment.getSite(), project),
                    equipment.getId(), formatSk(start), formatSk(end)))
            .isEmpty();

    if (hasRequiredFiles && hasMonitoring) {
        return end == null ? CompletenessStatus.IN_PROGRESS : CompletenessStatus.COMPLETE;
    }
    return end == null ? CompletenessStatus.IN_PROGRESS : CompletenessStatus.MISSING_FILES;
}
 
    private EligibilityStatus mapEligibility(ParticipatingProject participation) {
        if (participation == null || participation.getIsEligible() == null) {
            return EligibilityStatus.PENDING;
        }
        return participation.getIsEligible() ? EligibilityStatus.ELIGIBLE : EligibilityStatus.NOT_ELIGIBLE;
    }
 
    private String formatAddress(Site site) {
        return String.join(" ",
                nullToEmpty(site.getPrefecture()),
                nullToEmpty(site.getMunicipality()),
                nullToEmpty(site.getBlocknum()),
                nullToEmpty(site.getBuildingName())
        ).trim();
    }
 
    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
 
    private String resolveProjectName(Project project) {
        return null; // TODO: field nama project belum dikonfirmasi (projectNumber? field lain?)
    }
 
    private <T> Page<T> paginateInMemory(List<T> all, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<T> content = start >= all.size() ? List.of() : all.subList(start, end);
        return new PageImpl<>(content, pageable, all.size());
    }
 
    // ============================================================
    // rowKey encode/decode -- encode Integer, bukan String
    // ============================================================
 
    private String encodeRowKey(PeriodStatus type, Integer id) {
        String raw = type.name() + ":" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes());
    }
 
    private DecodedRowKey decodeRowKey(String rowKey) {
        String raw;
        try {
            raw = new String(Base64.getUrlDecoder().decode(rowKey));
        } catch (IllegalArgumentException e) {
            throw new RowNotFoundException(rowKey);
        }
        String[] parts = raw.split(":", 2);
        if (parts.length != 2) {
            throw new RowNotFoundException(rowKey);
        }
        try {
            return new DecodedRowKey(PeriodStatus.valueOf(parts[0]), Integer.valueOf(parts[1]));
        } catch (NumberFormatException e) {
            throw new RowNotFoundException(rowKey);
        }
    }
 
    private record DecodedRowKey(PeriodStatus type, Integer id) {}
    private record LivePreview(LocalDateTime periodStart, EnR002ReductionResult result) {}
 
    // ============================================================
    // helper classes for request/response
    // ============================================================
 
    public record RowsQueryRequest(String companyId, int page, int size) {
        private static final int DEFAULT_SIZE = 20;
        private static final int MAX_SIZE = 100;
 
        public RowsQueryRequest {
            if (page < 0) {
                page = 0;
            }
            if (size <= 0) {
                size = DEFAULT_SIZE;
            }
            if (size > MAX_SIZE) {
                size = MAX_SIZE;
            }
        }
 
        public static RowsQueryRequest of(String companyId, Integer page, Integer size) {
            return new RowsQueryRequest(
                    companyId,
                    page == null ? 0 : page,
                    size == null ? DEFAULT_SIZE : size
            );
        }
    }
 
    public record CompanyKobetsuKatsudoRequest(String projectId) {
        public static CompanyKobetsuKatsudoRequest of(String projectId) {
            return new CompanyKobetsuKatsudoRequest(projectId);
        }
    }
 
    public record PageResponse<T>(List<T> items, int page, int size, long totalItems) {
        public static <T> PageResponse<T> from(Page<T> springPage) {
            return new PageResponse<>(
                    springPage.getContent(), springPage.getNumber(),
                    springPage.getSize(), springPage.getTotalElements()
            );
        }
    }
 
    public static class RowNotFoundException extends RuntimeException {
        public RowNotFoundException(String rowKey) {
            super("Row not found: " + rowKey);
        }
    }
}
 