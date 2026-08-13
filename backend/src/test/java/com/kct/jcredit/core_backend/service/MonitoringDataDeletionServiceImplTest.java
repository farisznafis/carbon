package com.kct.jcredit.core_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.kct.jcredit.core_backend.config.MonitoringDataRetentionProperties;
import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.response.MonitoringDeletionCandidateListResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringDeletionCandidateRecordListResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringDeletionExecutionResponse;
import com.kct.jcredit.core_backend.dto.response.details.MonitoringDeletionCandidateDetailResponse;
import com.kct.jcredit.core_backend.entity.EnR002MonitoringRecord;
import com.kct.jcredit.core_backend.entity.MonitoringDataDeletionHistory;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
import com.kct.jcredit.core_backend.enums.DeletionResult;
import com.kct.jcredit.core_backend.repository.dynamodb.MonitoringTableRecordSolarRepository;
import com.kct.jcredit.core_backend.repository.postgres.MonitoringDataDeletionHistoryRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.service.impl.MonitoringDataDeletionServiceImpl;

import software.amazon.awssdk.enhanced.dynamodb.Key;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MonitoringDataDeletionServiceImplTest {

    private static final String FINALIZED_STATUS = "終了";

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private SiteProjectRelationRepository siteProjectRelationRepository;

    @Mock
    private MonitoringTableRecordSolarRepository monitoringTableRecordSolarRepository;

    @Mock
    private MonitoringDataDeletionHistoryRepository monitoringDataDeletionHistoryRepository;

    @Mock
    private MonitoringDataRetentionProperties retentionProperties;

    @InjectMocks
    private MonitoringDataDeletionServiceImpl service;

    private Project eligibleProject;
    private Site site;
    private SiteProjectRelation relation;

    @BeforeEach
    void setUp() {
        when(retentionProperties.getRetentionYears()).thenReturn(2);

        eligibleProject = buildProject(1, "PROJPUBLIC1", "eligible-project", LocalDateTime.now().minusYears(3));
        site = buildSite(10, "SITEPUBLIC1");
        relation = buildRelation(eligibleProject, site);
    }

    @Test
    @DisplayName("cutoff is computed from the configured retention years")
    void cutoffIsComputedFromConfig() {
        when(projectRepository.findByProjectStatusAndStatusFinalizedAtBefore(eq(FINALIZED_STATUS), any()))
                .thenReturn(List.of());

        service.previewDeletionCandidates();

        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(projectRepository).findByProjectStatusAndStatusFinalizedAtBefore(eq(FINALIZED_STATUS), cutoffCaptor.capture());

        LocalDateTime expectedCutoff = LocalDateTime.now().minusYears(2);
        assertTrue(java.time.Duration.between(cutoffCaptor.getValue(), expectedCutoff).abs().getSeconds() < 5);
    }

    @Test
    @DisplayName("PK is built as sitePublicId#projectPublicId")
    void pkIsBuiltFromSiteAndProjectPublicIds() {
        when(projectRepository.findByProjectStatusAndStatusFinalizedAtBefore(eq(FINALIZED_STATUS), any()))
                .thenReturn(List.of(eligibleProject));
        when(siteProjectRelationRepository.findByProject_Id(eligibleProject.getId()))
                .thenReturn(List.of(relation));
        when(monitoringTableRecordSolarRepository.findAllByPk(anyString())).thenReturn(List.of());

        service.previewDeletionCandidates();

        verify(monitoringTableRecordSolarRepository).findAllByPk("SITEPUBLIC1#PROJPUBLIC1");
    }

    @Test
    @DisplayName("records with a recent dateRecorded are excluded even when the project is eligible")
    void recentRecordsAreExcludedFromCandidates() {
        EnR002MonitoringRecord oldRecord = buildRecord("SITEPUBLIC1#PROJPUBLIC1", "2020-01-01T00:00:00Z", "2020-01-01", 1, 10);
        EnR002MonitoringRecord recentRecord = buildRecord("SITEPUBLIC1#PROJPUBLIC1", "2026-01-01T00:00:00Z", LocalDate.now().minusDays(1).toString(), 1, 10);

        when(projectRepository.findByProjectStatusAndStatusFinalizedAtBefore(eq(FINALIZED_STATUS), any()))
                .thenReturn(List.of(eligibleProject));
        when(siteProjectRelationRepository.findByProject_Id(eligibleProject.getId()))
                .thenReturn(List.of(relation));
        when(monitoringTableRecordSolarRepository.findAllByPk("SITEPUBLIC1#PROJPUBLIC1"))
                .thenReturn(List.of(oldRecord, recentRecord));

        MonitoringDeletionCandidateListResponse response = service.previewDeletionCandidates();

        assertEquals(1, response.getCandidates().size());
        MonitoringDeletionCandidateDetailResponse candidate = response.getCandidates().get(0);
        assertEquals(1, candidate.getCandidateRecordCount());
        assertEquals("2020-01-01", candidate.getOldestDateRecorded());
        assertEquals("2020-01-01", candidate.getNewestDateRecorded());
    }

    @Test
    @DisplayName("previewDeletionCandidateRecords returns sorted, filtered records for one project/site")
    void previewDeletionCandidateRecordsReturnsFilteredRecords() {
        EnR002MonitoringRecord oldRecord = buildRecord("SITEPUBLIC1#PROJPUBLIC1", "2020-02-01T00:00:00Z", "2020-02-01", 1, 10);
        EnR002MonitoringRecord olderRecord = buildRecord("SITEPUBLIC1#PROJPUBLIC1", "2020-01-01T00:00:00Z", "2020-01-01", 1, 10);
        EnR002MonitoringRecord recentRecord = buildRecord("SITEPUBLIC1#PROJPUBLIC1", "2026-01-01T00:00:00Z", LocalDate.now().minusDays(1).toString(), 1, 10);

        when(projectRepository.findByProjectStatusAndStatusFinalizedAtBefore(eq(FINALIZED_STATUS), any()))
                .thenReturn(List.of(eligibleProject));
        when(siteProjectRelationRepository.findByProject_Id(eligibleProject.getId()))
                .thenReturn(List.of(relation));
        when(monitoringTableRecordSolarRepository.findAllByPk("SITEPUBLIC1#PROJPUBLIC1"))
                .thenReturn(List.of(oldRecord, olderRecord, recentRecord));

        MonitoringDeletionCandidateRecordListResponse response =
                service.previewDeletionCandidateRecords("PROJPUBLIC1", "SITEPUBLIC1");

        assertEquals(ResponseConstants.OK, response.getStatusCode());
        assertEquals(2, response.getTotalRecords());
        assertEquals("2020-01-01", response.getRecords().get(0).getDateRecorded());
        assertEquals("2020-02-01", response.getRecords().get(1).getDateRecorded());
    }

    @Test
    @DisplayName("previewDeletionCandidateRecords returns NOT_FOUND when the project is not an eligible candidate")
    void previewDeletionCandidateRecordsRejectsNonEligibleProject() {
        when(projectRepository.findByProjectStatusAndStatusFinalizedAtBefore(eq(FINALIZED_STATUS), any()))
                .thenReturn(List.of());

        MonitoringDeletionCandidateRecordListResponse response =
                service.previewDeletionCandidateRecords("PROJPUBLIC1", "SITEPUBLIC1");

        assertEquals(ResponseConstants.NOT_FOUND, response.getStatusCode());
        verify(siteProjectRelationRepository, never()).findByProject_Id(any());
    }

    @Test
    @DisplayName("previewDeletionCandidateRecords returns NOT_FOUND when the site is not related to the project")
    void previewDeletionCandidateRecordsRejectsUnrelatedSite() {
        when(projectRepository.findByProjectStatusAndStatusFinalizedAtBefore(eq(FINALIZED_STATUS), any()))
                .thenReturn(List.of(eligibleProject));
        when(siteProjectRelationRepository.findByProject_Id(eligibleProject.getId()))
                .thenReturn(List.of(relation));

        MonitoringDeletionCandidateRecordListResponse response =
                service.previewDeletionCandidateRecords("PROJPUBLIC1", "SITE_UNRELATED");

        assertEquals(ResponseConstants.NOT_FOUND, response.getStatusCode());
        verify(monitoringTableRecordSolarRepository, never()).findAllByPk(anyString());
    }

    @Test
    @DisplayName("deleteBatch is called exactly once with all candidate keys instead of per-record delete")
    void deleteBatchCalledOnceWithAllKeys() {
        EnR002MonitoringRecord record1 = buildRecord("SITEPUBLIC1#PROJPUBLIC1", "2020-01-01T00:00:00Z", "2020-01-01", 1, 10);
        EnR002MonitoringRecord record2 = buildRecord("SITEPUBLIC1#PROJPUBLIC1", "2020-02-01T00:00:00Z", "2020-02-01", 1, 10);

        when(projectRepository.findByProjectStatusAndStatusFinalizedAtBefore(eq(FINALIZED_STATUS), any()))
                .thenReturn(List.of(eligibleProject));
        when(siteProjectRelationRepository.findByProject_Id(eligibleProject.getId()))
                .thenReturn(List.of(relation));
        when(monitoringTableRecordSolarRepository.findAllByPk("SITEPUBLIC1#PROJPUBLIC1"))
                .thenReturn(List.of(record1, record2));
        when(monitoringTableRecordSolarRepository.deleteBatch(any())).thenReturn(List.of());

        service.executeDeletion("operator1", List.of("PROJPUBLIC1"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Key>> keysCaptor = ArgumentCaptor.forClass(List.class);
        verify(monitoringTableRecordSolarRepository, times(1)).deleteBatch(keysCaptor.capture());
        assertEquals(2, keysCaptor.getValue().size());
        verify(monitoringTableRecordSolarRepository, never()).delete(anyString(), anyString());
    }

    @Test
    @DisplayName("keys returned as unprocessed are recorded as FAILURE, the rest as SUCCESS")
    void unprocessedKeysAreRecordedAsFailure() {
        EnR002MonitoringRecord record1 = buildRecord("SITEPUBLIC1#PROJPUBLIC1", "2020-01-01T00:00:00Z", "2020-01-01", 1, 10);
        EnR002MonitoringRecord record2 = buildRecord("SITEPUBLIC1#PROJPUBLIC1", "2020-02-01T00:00:00Z", "2020-02-01", 1, 10);

        when(projectRepository.findByProjectStatusAndStatusFinalizedAtBefore(eq(FINALIZED_STATUS), any()))
                .thenReturn(List.of(eligibleProject));
        when(siteProjectRelationRepository.findByProject_Id(eligibleProject.getId()))
                .thenReturn(List.of(relation));
        when(monitoringTableRecordSolarRepository.findAllByPk("SITEPUBLIC1#PROJPUBLIC1"))
                .thenReturn(List.of(record1, record2));

        Key failedKey = Key.builder()
                .partitionValue(record2.getId())
                .sortValue(record2.getTimeUpdated())
                .build();
        when(monitoringTableRecordSolarRepository.deleteBatch(any())).thenReturn(List.of(failedKey));

        MonitoringDeletionExecutionResponse response = service.executeDeletion("operator1", List.of("PROJPUBLIC1"));

        assertEquals(1, response.getDeletedCount());
        assertEquals(1, response.getFailedCount());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MonitoringDataDeletionHistory>> historyCaptor = ArgumentCaptor.forClass(List.class);
        verify(monitoringDataDeletionHistoryRepository).saveAll(historyCaptor.capture());

        List<MonitoringDataDeletionHistory> savedHistory = historyCaptor.getValue();
        assertEquals(2, savedHistory.size());
        long successCount = savedHistory.stream().filter(h -> h.getResult() == DeletionResult.SUCCESS).count();
        long failureCount = savedHistory.stream().filter(h -> h.getResult() == DeletionResult.FAILURE).count();
        assertEquals(1, successCount);
        assertEquals(1, failureCount);
    }

    @Test
    @DisplayName("executeDeletion only processes projects that are both eligible and requested")
    void executeDeletionOnlyProcessesRequestedEligibleProjects() {
        Project otherEligibleProject = buildProject(2, "PROJPUBLIC2", "other-eligible-project", LocalDateTime.now().minusYears(3));

        when(projectRepository.findByProjectStatusAndStatusFinalizedAtBefore(eq(FINALIZED_STATUS), any()))
                .thenReturn(List.of(eligibleProject, otherEligibleProject));
        when(siteProjectRelationRepository.findByProject_Id(eligibleProject.getId()))
                .thenReturn(List.of(relation));
        when(monitoringTableRecordSolarRepository.findAllByPk("SITEPUBLIC1#PROJPUBLIC1"))
                .thenReturn(List.of(buildRecord("SITEPUBLIC1#PROJPUBLIC1", "2020-01-01T00:00:00Z", "2020-01-01", 1, 10)));
        when(monitoringTableRecordSolarRepository.deleteBatch(any())).thenReturn(List.of());

        service.executeDeletion("operator1", List.of("PROJPUBLIC1"));

        verify(siteProjectRelationRepository, never()).findByProject_Id(otherEligibleProject.getId());
    }

    @Test
    @DisplayName("findDeletionHistory delegates to the repository with a page/limit pageable")
    void findDeletionHistoryDelegatesToRepository() {
        MonitoringDataDeletionHistory history = MonitoringDataDeletionHistory.builder()
                .monitoringRecordId("SITEPUBLIC1#PROJPUBLIC1")
                .recordTimeUpdated("2020-01-01T00:00:00Z")
                .dateRecorded("2020-01-01")
                .projectId(1)
                .siteId(10)
                .result(DeletionResult.SUCCESS)
                .build();
        Page<MonitoringDataDeletionHistory> page = new PageImpl<>(List.of(history));
        when(monitoringDataDeletionHistoryRepository.findByProjectId(eq(1), any(Pageable.class))).thenReturn(page);

        var response = service.findDeletionHistory(1, 1, 20);

        assertEquals(1, response.getHistory().size());
        verify(monitoringDataDeletionHistoryRepository).findByProjectId(eq(1), any(Pageable.class));
    }

    private Project buildProject(Integer id, String publicId, String name, LocalDateTime statusFinalizedAt) {
        Project project = new Project();
        project.setId(id);
        project.setPublicId(publicId);
        project.setProjectName(name);
        project.setProjectStatus(FINALIZED_STATUS);
        project.setStatusFinalizedAt(statusFinalizedAt);
        return project;
    }

    private Site buildSite(Integer id, String publicId) {
        Site s = new Site();
        s.setId(id);
        s.setPublicId(publicId);
        return s;
    }

    private SiteProjectRelation buildRelation(Project project, Site site) {
        SiteProjectRelation r = new SiteProjectRelation();
        r.setProject(project);
        r.setSite(site);
        return r;
    }

    private EnR002MonitoringRecord buildRecord(String pk, String timeUpdated, String dateRecorded, Integer projectId, Integer siteId) {
        EnR002MonitoringRecord record = new EnR002MonitoringRecord();
        record.setId(pk);
        record.setTimeUpdated(timeUpdated);
        record.setDateRecorded(dateRecorded);
        record.setProjectID(projectId);
        record.setSiteID(siteId);
        return record;
    }
}
