package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.config.MonitoringDataRetentionProperties;
import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.response.MonitoringDataDeletionHistoryListResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringDeletionScheduleListResponse;
import com.kct.jcredit.core_backend.dto.response.details.MonitoringDataDeletionHistoryDetailResponse;
import com.kct.jcredit.core_backend.dto.response.details.MonitoringDeletionScheduleDetailResponse;
import com.kct.jcredit.core_backend.entity.EnR002MonitoringRecord;
import com.kct.jcredit.core_backend.entity.MonitoringDataDeletionHistory;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.base.PaginationDetailResponse;
import com.kct.jcredit.core_backend.enums.DeletionResult;
import com.kct.jcredit.core_backend.repository.dynamodb.MonitoringTableRecordSolarRepository;
import com.kct.jcredit.core_backend.repository.postgres.MonitoringDataDeletionHistoryRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository.LinkedSite;
import com.kct.jcredit.core_backend.service.MonitoringDataDeletionService;
import com.kct.jcredit.core_backend.service.NotificationMessages;
import com.kct.jcredit.core_backend.service.NotificationService;
import com.kct.jcredit.core_backend.service.ProjectService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringDataDeletionServiceImpl implements MonitoringDataDeletionService {

    private static final String FINALIZED_PROJECT_STATUS = "終了";
    private static final String UNPROCESSED_FAILURE_REASON = "DynamoDB throttled/unprocessed";
    // DynamoDB BatchWriteItem accepts at most 25 items, so one page is exactly one batch.
    private static final int BATCH_SIZE = 25;
    private static final int MAX_UNPROCESSED_RETRIES = 5;
    private static final int FAILURE_REASON_MAX_LENGTH = 500;

    private final ProjectRepository projectRepository;
    private final SiteProjectRelationRepository siteProjectRelationRepository;
    private final MonitoringTableRecordSolarRepository monitoringTableRecordSolarRepository;
    private final MonitoringDataDeletionHistoryRepository monitoringDataDeletionHistoryRepository;
    private final MonitoringDataRetentionProperties retentionProperties;
    private final NotificationService notificationService;
    private final NotificationMessages messages;
    private final ProjectService projectService;

    @Override
    @Transactional(readOnly = true)
    public MonitoringDeletionScheduleListResponse findDeletionSchedule() {
        LocalDate today = LocalDate.now();
        LocalDate earliestDeletionDate = retentionProperties.resolveEarliestDeletionDate();

        List<MonitoringDeletionScheduleDetailResponse> schedule = new ArrayList<>();
        for (Project project : finalizedProjects()) {
            LocalDate deletionDate = deletionDate(project, earliestDeletionDate);
            if (deletionDate.isBefore(today)) {
                continue;
            }
            schedule.add(MonitoringDeletionScheduleDetailResponse.builder()
                    .projectId(project.getId())
                    .projectPublicId(project.getPublicId())
                    .projectName(project.getProjectName())
                    .statusFinalizedAt(project.getStatusFinalizedAt())
                    .scheduledDeletionDate(deletionDate)
                    .daysUntilDeletion(ChronoUnit.DAYS.between(today, deletionDate))
                    .build());
        }
        schedule.sort(Comparator.comparing(MonitoringDeletionScheduleDetailResponse::getScheduledDeletionDate)
                .thenComparing(MonitoringDeletionScheduleDetailResponse::getProjectId));

        MonitoringDeletionScheduleListResponse response = new MonitoringDeletionScheduleListResponse();
        response.setStatusCode(ResponseConstants.OK);
        response.setMessage(ResponseConstants.MSG_MONITORING_DELETION_SCHEDULE_RETRIEVED);
        response.setAutoDeletionEnabled(retentionProperties.isAutoDeletionEnabled() && earliestDeletionDate != null);
        response.setTotalProjects(schedule.size());
        response.setSchedule(schedule);
        return response;
    }

    // Deliberately not @Transactional: DynamoDB deletes cannot be rolled back, so each batch's
    // history is committed on its own instead of being lost with a failure later in the run.
    @Override
    public void runAutomaticDeletion(LocalDate today) {
        LocalDate earliestDeletionDate = retentionProperties.resolveEarliestDeletionDate();
        if (earliestDeletionDate == null) {
            log.warn("[MONITORING DELETION] Skipped: app.monitoring.retention.earliest-deletion-date is not set");
            return;
        }
        LocalDate advanceNoticeDate = today.plusDays(retentionProperties.getAdvanceNoticeDays());

        List<Project> advanceNotice = new ArrayList<>();
        List<Project> startingToday = new ArrayList<>();
        List<Project> due = new ArrayList<>();
        for (Project project : finalizedProjects()) {
            LocalDate deletionDate = deletionDate(project, earliestDeletionDate);
            if (deletionDate.equals(today)) {
                startingToday.add(project);
            } else if (deletionDate.equals(advanceNoticeDate)) {
                advanceNotice.add(project);
            }
            if (!deletionDate.isAfter(today)) {
                due.add(project);
            }
        }

        notifyProjects("monitoring.deletion.advance", advanceNoticeDate, advanceNotice);
        notifyProjects("monitoring.deletion.started", today, startingToday);

        for (Project project : due) {
            deleteProjectData(project);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MonitoringDataDeletionHistoryListResponse findDeletionHistory(
            Integer projectId, int page, int limit, String currentUserId) {
        Pageable pageable = PageRequest.of(
                page - 1,
                limit,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))
        );

        Page<MonitoringDataDeletionHistory> historyPage;
        if (projectId != null) {
            Project project = projectRepository.findById(projectId).orElse(null);
            // An unknown project and a project the caller may not see get the same answer,
            // so the response does not reveal which ids exist.
            if (project == null || !projectService.hasProjectAccess(project.getPublicId(), currentUserId)) {
                MonitoringDataDeletionHistoryListResponse notFound =
                        new MonitoringDataDeletionHistoryListResponse();
                notFound.setStatusCode(ResponseConstants.NOT_FOUND);
                return notFound;
            }
            historyPage = monitoringDataDeletionHistoryRepository.findByProjectId(projectId, pageable);
        } else {
            // No project given: restrict to the projects the caller may see. History only
            // exists for finalized projects, so finalizedProjects() is the candidate set.
            List<Integer> accessibleProjectIds = finalizedProjects().stream()
                    .filter(project -> projectService.hasProjectAccess(project.getPublicId(), currentUserId))
                    .map(Project::getId)
                    .toList();
            historyPage = accessibleProjectIds.isEmpty()
                    ? Page.empty(pageable)
                    : monitoringDataDeletionHistoryRepository.findByProjectIdIn(accessibleProjectIds, pageable);
        }

        MonitoringDataDeletionHistoryListResponse response = new MonitoringDataDeletionHistoryListResponse();
        response.setStatusCode(ResponseConstants.OK);
        response.setMessage(ResponseConstants.MSG_MONITORING_DELETION_HISTORY_RETRIEVED);
        response.setHistory(historyPage.getContent().stream().map(this::toHistoryDetail).toList());
        response.setPagination(new PaginationDetailResponse(
                page,
                limit,
                historyPage.getTotalElements(),
                historyPage.getTotalPages(),
                null,
                historyPage.hasNext()
        ));
        return response;
    }

    private List<Project> finalizedProjects() {
        return projectRepository.findByProjectStatus(FINALIZED_PROJECT_STATUS).stream()
                .filter(project -> project.getStatusFinalizedAt() != null)
                .toList();
    }

    private LocalDate deletionDate(Project project, LocalDate earliestDeletionDate) {
        LocalDate retentionEnd = project.getStatusFinalizedAt().toLocalDate()
                .plusYears(retentionProperties.getRetentionYears());
        return earliestDeletionDate != null && earliestDeletionDate.isAfter(retentionEnd)
                ? earliestDeletionDate
                : retentionEnd;
    }

    /**
     * Deletes everything stored for the project, one site partition at a time. Nothing is kept
     * between runs: a partition that still has items is simply walked again by the next run, so
     * a project that is already empty costs one query per site and nothing else.
     */
    private void deleteProjectData(Project project) {
        long deletedCount = 0;
        boolean fullyDeleted = true;

        for (LinkedSite site : siteProjectRelationRepository.findLinkedSitesIncludingDeleted(project.getId())) {
            String pk = site.getSitePublicId() + "#" + project.getPublicId();
            try {
                for (List<EnR002MonitoringRecord> batch
                        : monitoringTableRecordSolarRepository.findPagesByPk(pk, BATCH_SIZE)) {
                    if (batch.isEmpty()) {
                        continue;
                    }
                    int failedCount = deleteBatch(batch, project, site);
                    deletedCount += batch.size() - failedCount;
                    if (failedCount > 0) {
                        fullyDeleted = false;
                        break;
                    }
                }
            } catch (RuntimeException e) {
                fullyDeleted = false;
                log.error("[MONITORING DELETION] Failed on partition {} of project {}", pk, project.getPublicId(), e);
            }
        }

        if (!fullyDeleted) {
            log.warn("[MONITORING DELETION] Project {} still has monitoring data after {} deletions; the next run retries",
                    project.getPublicId(), deletedCount);
            return;
        }
        // Only the run that removes the last records reports completion, so it is sent once.
        if (deletedCount > 0) {
            long totalDeleted = monitoringDataDeletionHistoryRepository
                    .countByProjectIdAndResult(project.getId(), DeletionResult.SUCCESS);
            log.info("[MONITORING DELETION] Project {} completed, {} records deleted in total",
                    project.getPublicId(), totalDeleted);
            sendToOperators(messages.of("monitoring.deletion.completed",
                    project.getProjectName(), project.getPublicId(), String.valueOf(totalDeleted)));
        }
    }

    /**
     * Deletes one batch, retrying whatever DynamoDB hands back as unprocessed, and records the
     * outcome of every record. Returns how many records are still not deleted.
     *
     * TODO(S3 evidence): the evidence files these records point to are left in S3 on purpose,
     * pending confirmation that they must go too. If they must:
     *  1. Before deleting the batch, collect the non-null values of elPvEvidenceUrl,
     *     elPvrEvidenceUrl, elpjBatterySpecificationUrl and elpjControlEvidenceUrl from each
     *     record. This is the only chance: once the record is gone nothing references the file.
     *     Check whether they hold a bare S3 key or a full URL and reduce them to a key.
     *  2. Delete the files first with S3FileStorage.delete(key), then the records. A failed
     *     file delete must keep its record (count it as failed) so the next run retries both;
     *     the other order leaves orphan files that can never be found again.
     *  3. Skip a key still referenced by a record outside this deletion, e.g. an equipment
     *     spec sheet shared by several records, or by the equipment master.
     *  4. Offset records created by 取消 have these fields nulled, so they need nothing.
     *  5. monitoring_data_deletion_history has no column for a file key and the schema must
     *     stay as it is, so file deletions would be traceable in the application log only.
     */
    private int deleteBatch(List<EnR002MonitoringRecord> batch, Project project, LinkedSite site) {
        Map<Key, EnR002MonitoringRecord> keyToRecord = new LinkedHashMap<>();
        for (EnR002MonitoringRecord record : batch) {
            keyToRecord.put(Key.builder()
                    .partitionValue(record.getId())
                    .sortValue(record.getTimeUpdated())
                    .build(), record);
        }

        List<Key> pending = new ArrayList<>(keyToRecord.keySet());
        String failureReason = null;
        try {
            pending = monitoringTableRecordSolarRepository.deleteBatch(pending);
            for (int attempt = 1; !pending.isEmpty() && attempt <= MAX_UNPROCESSED_RETRIES; attempt++) {
                pauseBeforeRetry(attempt);
                pending = monitoringTableRecordSolarRepository.deleteBatch(pending);
            }
            if (!pending.isEmpty()) {
                failureReason = UNPROCESSED_FAILURE_REASON;
            }
        } catch (RuntimeException e) {
            failureReason = truncate(
            e.getMessage() == null || e.getMessage().isBlank()
                    ? e.getClass().getName()
                    : e.getMessage());
            log.error("[MONITORING DELETION] Batch delete failed for project {}", project.getPublicId(), e);
        }

        Set<Key> failed = new HashSet<>(pending);
        List<MonitoringDataDeletionHistory> historyRows = new ArrayList<>();
        for (Map.Entry<Key, EnR002MonitoringRecord> entry : keyToRecord.entrySet()) {
            EnR002MonitoringRecord record = entry.getValue();
            boolean isFailed = failed.contains(entry.getKey());
            historyRows.add(MonitoringDataDeletionHistory.builder()
                    .monitoringRecordId(record.getId())
                    .recordTimeUpdated(record.getTimeUpdated())
                    .dateRecorded(record.getDateRecorded())
                    .projectId(project.getId())
                    .siteId(site.getSiteId())
                    .result(isFailed ? DeletionResult.FAILURE : DeletionResult.SUCCESS)
                    .failureReason(isFailed ? failureReason : null)
                    .build());
        }
        monitoringDataDeletionHistoryRepository.saveAll(historyRows);
        return failed.size();
    }

    private void pauseBeforeRetry(int attempt) {
        long backoffMillis = retentionProperties.getRetryBackoffMillis() << (attempt - 1);
        if (backoffMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(backoffMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to retry unprocessed deletes", e);
        }
    }

    private void notifyProjects(String messageKey, LocalDate deletionDate, List<Project> projects) {
        if (projects.isEmpty()) {
            return;
        }
        String projectLines = projects.stream()
                .map(project -> messages.text("monitoring.deletion.project-line",
                        project.getProjectName(), project.getPublicId()))
                .collect(Collectors.joining("\n"));
        sendToOperators(messages.of(messageKey, deletionDate.toString(), projectLines));
    }

    // A notification that cannot be delivered must never stop the deletion itself.
    private void sendToOperators(NotificationMessages.Content content) {
        try {
            notificationService.createNotificationForAllOperatingUsers(content.subject(), content.body());
        } catch (Exception e) {
            log.warn("[MONITORING DELETION] In-app notification failed: {}", e.getMessage());
        }
        try {
            notificationService.sendEmailToAllOperatingUsers(content.subject(), content.body());
        } catch (Exception e) {
            log.warn("[MONITORING DELETION] Email notification failed: {}", e.getMessage());
        }
    }

    private String truncate(String text) {
        return text.length() <= FAILURE_REASON_MAX_LENGTH ? text : text.substring(0, FAILURE_REASON_MAX_LENGTH);
    }

    private MonitoringDataDeletionHistoryDetailResponse toHistoryDetail(MonitoringDataDeletionHistory history) {
        return MonitoringDataDeletionHistoryDetailResponse.builder()
                .id(history.getId())
                .monitoringRecordId(history.getMonitoringRecordId())
                .recordTimeUpdated(history.getRecordTimeUpdated())
                .dateRecorded(history.getDateRecorded())
                .projectId(history.getProjectId())
                .siteId(history.getSiteId())
                .result(history.getResult())
                .failureReason(history.getFailureReason())
                .deletedAt(history.getCreatedAt())
                .deletedBy(history.getCreatedBy())
                .build();
    }
}
