package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.config.MonitoringDataRetentionProperties;
import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.response.MonitoringDataDeletionHistoryListResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringDeletionCandidateListResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringDeletionCandidateRecordListResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringDeletionExecutionResponse;
import com.kct.jcredit.core_backend.dto.response.details.MonitoringDataDeletionHistoryDetailResponse;
import com.kct.jcredit.core_backend.dto.response.details.MonitoringDeletionCandidateDetailResponse;
import com.kct.jcredit.core_backend.dto.response.details.MonitoringDeletionCandidateRecordDetailResponse;
import com.kct.jcredit.core_backend.entity.EnR002MonitoringRecord;
import com.kct.jcredit.core_backend.entity.MonitoringDataDeletionHistory;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
import com.kct.jcredit.core_backend.entity.base.PaginationDetailResponse;
import com.kct.jcredit.core_backend.enums.DeletionResult;
import com.kct.jcredit.core_backend.repository.dynamodb.MonitoringTableRecordSolarRepository;
import com.kct.jcredit.core_backend.repository.postgres.MonitoringDataDeletionHistoryRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.service.MonitoringDataDeletionService;

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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringDataDeletionServiceImpl implements MonitoringDataDeletionService {

    private static final String FINALIZED_PROJECT_STATUS = "終了";
    private static final String UNPROCESSED_FAILURE_REASON = "DynamoDB throttled/unprocessed";

    private final ProjectRepository projectRepository;
    private final SiteProjectRelationRepository siteProjectRelationRepository;
    private final MonitoringTableRecordSolarRepository monitoringTableRecordSolarRepository;
    private final MonitoringDataDeletionHistoryRepository monitoringDataDeletionHistoryRepository;
    private final MonitoringDataRetentionProperties retentionProperties;

    @Override
    @Transactional(readOnly = true)
    public MonitoringDeletionCandidateListResponse previewDeletionCandidates() {
        LocalDate cutoffDate = currentCutoff().toLocalDate();
        List<Project> eligibleProjects = eligibleProjects();

        List<MonitoringDeletionCandidateDetailResponse> candidates = new ArrayList<>();
        long totalRecords = 0;

        for (Project project : eligibleProjects) {
            for (SiteProjectRelation relation : siteProjectRelationRepository.findByProject_Id(project.getId())) {
                List<EnR002MonitoringRecord> eligibleRecords =
                        filterEligibleRecords(fetchRecords(relation, project), cutoffDate);
                if (eligibleRecords.isEmpty()) {
                    continue;
                }

                String oldest = eligibleRecords.stream()
                        .map(EnR002MonitoringRecord::getDateRecorded)
                        .min(String::compareTo)
                        .orElse(null);
                String newest = eligibleRecords.stream()
                        .map(EnR002MonitoringRecord::getDateRecorded)
                        .max(String::compareTo)
                        .orElse(null);

                candidates.add(MonitoringDeletionCandidateDetailResponse.builder()
                        .projectId(project.getId())
                        .projectPublicId(project.getPublicId())
                        .projectName(project.getProjectName())
                        .siteId(relation.getSite().getId())
                        .sitePublicId(relation.getSite().getPublicId())
                        .statusFinalizedAt(project.getStatusFinalizedAt())
                        .candidateRecordCount(eligibleRecords.size())
                        .oldestDateRecorded(oldest)
                        .newestDateRecorded(newest)
                        .build());
                totalRecords += eligibleRecords.size();
            }
        }

        MonitoringDeletionCandidateListResponse response = new MonitoringDeletionCandidateListResponse();
        response.setStatusCode(ResponseConstants.OK);
        response.setMessage(candidates.isEmpty()
                ? ResponseConstants.MSG_MONITORING_DELETION_NO_CANDIDATES
                : ResponseConstants.MSG_MONITORING_DELETION_CANDIDATES_RETRIEVED);
        response.setTotalProjects(candidates.stream()
                .map(MonitoringDeletionCandidateDetailResponse::getProjectId)
                .distinct()
                .count());
        response.setTotalRecords(totalRecords);
        response.setCandidates(candidates);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public MonitoringDeletionCandidateRecordListResponse previewDeletionCandidateRecords(
            String projectPublicId,
            String sitePublicId) {
        LocalDate cutoffDate = currentCutoff().toLocalDate();
        MonitoringDeletionCandidateRecordListResponse response = new MonitoringDeletionCandidateRecordListResponse();

        Project project = eligibleProjects().stream()
                .filter(p -> p.getPublicId().equals(projectPublicId))
                .findFirst()
                .orElse(null);
        if (project == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_MONITORING_DELETION_PROJECT_NOT_CANDIDATE);
            return response;
        }

        SiteProjectRelation relation = siteProjectRelationRepository.findByProject_Id(project.getId()).stream()
                .filter(r -> r.getSite().getPublicId().equals(sitePublicId))
                .findFirst()
                .orElse(null);
        if (relation == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_SITE_PROJECT_RELATION_NOT_FOUND);
            return response;
        }

        List<EnR002MonitoringRecord> eligibleRecords = filterEligibleRecords(fetchRecords(relation, project), cutoffDate)
                .stream()
                .sorted(Comparator.comparing(EnR002MonitoringRecord::getDateRecorded))
                .toList();

        response.setStatusCode(ResponseConstants.OK);
        response.setMessage(eligibleRecords.isEmpty()
                ? ResponseConstants.MSG_MONITORING_DELETION_NO_CANDIDATES
                : ResponseConstants.MSG_MONITORING_DELETION_CANDIDATE_RECORDS_RETRIEVED);
        response.setProjectId(project.getId());
        response.setProjectPublicId(project.getPublicId());
        response.setSiteId(relation.getSite().getId());
        response.setSitePublicId(relation.getSite().getPublicId());
        response.setTotalRecords(eligibleRecords.size());
        response.setRecords(eligibleRecords.stream().map(this::toCandidateRecordDetail).toList());
        return response;
    }

    @Override
    @Transactional
    public MonitoringDeletionExecutionResponse executeDeletion(String actor, List<String> projectPublicIds) {
        LocalDate cutoffDate = currentCutoff().toLocalDate();
        Set<String> requestedPublicIds = new HashSet<>(projectPublicIds);
        List<Project> targetProjects = eligibleProjects().stream()
                .filter(project -> requestedPublicIds.contains(project.getPublicId()))
                .toList();

        List<Key> allKeys = new ArrayList<>();
        Map<Key, EnR002MonitoringRecord> keyToRecord = new HashMap<>();

        for (Project project : targetProjects) {
            for (SiteProjectRelation relation : siteProjectRelationRepository.findByProject_Id(project.getId())) {
                List<EnR002MonitoringRecord> eligibleRecords =
                        filterEligibleRecords(fetchRecords(relation, project), cutoffDate);
                for (EnR002MonitoringRecord record : eligibleRecords) {
                    Key key = Key.builder()
                            .partitionValue(record.getId())
                            .sortValue(record.getTimeUpdated())
                            .build();
                    allKeys.add(key);
                    keyToRecord.put(key, record);
                }
            }
        }

        MonitoringDeletionExecutionResponse response = new MonitoringDeletionExecutionResponse();
        response.setExecutedAt(LocalDateTime.now());
        response.setExecutedBy(actor);

        if (allKeys.isEmpty()) {
            response.setStatusCode(ResponseConstants.OK);
            response.setMessage(ResponseConstants.MSG_MONITORING_DELETION_NO_CANDIDATES);
            response.setDeletedCount(0);
            response.setFailedCount(0);
            return response;
        }

        Set<Key> unprocessed = new HashSet<>(monitoringTableRecordSolarRepository.deleteBatch(allKeys));

        List<MonitoringDataDeletionHistory> historyRows = new ArrayList<>();
        int deletedCount = 0;
        int failedCount = 0;

        for (Key key : allKeys) {
            EnR002MonitoringRecord record = keyToRecord.get(key);
            boolean failed = unprocessed.contains(key);

            historyRows.add(MonitoringDataDeletionHistory.builder()
                    .monitoringRecordId(record.getId())
                    .recordTimeUpdated(record.getTimeUpdated())
                    .dateRecorded(record.getDateRecorded())
                    .projectId(record.getProjectID())
                    .siteId(record.getSiteID())
                    .result(failed ? DeletionResult.FAILURE : DeletionResult.SUCCESS)
                    .failureReason(failed ? UNPROCESSED_FAILURE_REASON : null)
                    .build());

            if (failed) {
                failedCount++;
            } else {
                deletedCount++;
            }
        }
        monitoringDataDeletionHistoryRepository.saveAll(historyRows);

        response.setStatusCode(ResponseConstants.OK);
        response.setMessage(ResponseConstants.MSG_MONITORING_DELETION_COMPLETED);
        response.setDeletedCount(deletedCount);
        response.setFailedCount(failedCount);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public MonitoringDataDeletionHistoryListResponse findDeletionHistory(Integer projectId, int page, int limit) {
        Pageable pageable = PageRequest.of(
                page - 1,
                limit,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))
        );
        Page<MonitoringDataDeletionHistory> historyPage = projectId != null
                ? monitoringDataDeletionHistoryRepository.findByProjectId(projectId, pageable)
                : monitoringDataDeletionHistoryRepository.findAll(pageable);

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

    private LocalDateTime currentCutoff() {
        return LocalDateTime.now().minusYears(retentionProperties.getRetentionYears());
    }

    private List<Project> eligibleProjects() {
        return projectRepository.findByProjectStatusAndStatusFinalizedAtBefore(
                FINALIZED_PROJECT_STATUS,
                currentCutoff()
        );
    }

    private List<EnR002MonitoringRecord> fetchRecords(SiteProjectRelation relation, Project project) {
        String pk = relation.getSite().getPublicId() + "#" + project.getPublicId();
        return monitoringTableRecordSolarRepository.findAllByPk(pk);
    }

    private List<EnR002MonitoringRecord> filterEligibleRecords(
            List<EnR002MonitoringRecord> records,
            LocalDate cutoffDate) {
        List<EnR002MonitoringRecord> eligible = new ArrayList<>();
        for (EnR002MonitoringRecord record : records) {
            if (record.getDateRecorded() == null) {
                continue;
            }
            LocalDate dateRecorded;
            try {
                dateRecorded = LocalDate.parse(record.getDateRecorded());
            } catch (Exception e) {
                continue;
            }
            if (dateRecorded.isBefore(cutoffDate)) {
                eligible.add(record);
            }
        }
        return eligible;
    }

    private MonitoringDeletionCandidateRecordDetailResponse toCandidateRecordDetail(EnR002MonitoringRecord record) {
        return MonitoringDeletionCandidateRecordDetailResponse.builder()
                .monitoringRecordId(record.getId())
                .recordTimeUpdated(record.getTimeUpdated())
                .dateRecorded(record.getDateRecorded())
                .build();
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
