package com.kct.jcredit.core_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.kct.jcredit.core_backend.config.MonitoringDataRetentionProperties;
import com.kct.jcredit.core_backend.dto.response.MonitoringDeletionScheduleListResponse;
import com.kct.jcredit.core_backend.entity.EnR002MonitoringRecord;
import com.kct.jcredit.core_backend.entity.MonitoringDataDeletionHistory;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.enums.DeletionResult;
import com.kct.jcredit.core_backend.repository.dynamodb.MonitoringTableRecordSolarRepository;
import com.kct.jcredit.core_backend.repository.postgres.MonitoringDataDeletionHistoryRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository.LinkedSite;
import com.kct.jcredit.core_backend.service.impl.MonitoringDataDeletionServiceImpl;

import software.amazon.awssdk.enhanced.dynamodb.Key;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MonitoringDataDeletionServiceImplTest {

    private static final String FINALIZED_STATUS = "終了";
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 1);
    private static final String PK = "SITEPUBLIC1#PROJPUBLIC1";

    private static final String ADVANCE_SUBJECT = "モニタリングデータ削除予定のお知らせ";
    private static final String STARTED_SUBJECT = "モニタリングデータ削除開始のお知らせ";
    private static final String COMPLETED_SUBJECT = "モニタリングデータ削除完了のお知らせ";

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

    @Mock
    private NotificationService notificationService;

    // Real instance, so the tests also prove the message keys exist in mail/messages.properties.
    @Spy
    private NotificationMessages messages = new NotificationMessages();

    @InjectMocks
    private MonitoringDataDeletionServiceImpl service;

    @BeforeEach
    void setUp() {
        when(retentionProperties.getRetentionYears()).thenReturn(2);
        when(retentionProperties.getAdvanceNoticeDays()).thenReturn(7);
        when(retentionProperties.getRetryBackoffMillis()).thenReturn(0L);
        when(retentionProperties.resolveEarliestDeletionDate()).thenReturn(LocalDate.of(2000, 1, 1));
    }

    @Test
    @DisplayName("the job does nothing while the earliest deletion date is not configured")
    void skipsWhenEarliestDeletionDateIsMissing() {
        when(retentionProperties.resolveEarliestDeletionDate()).thenReturn(null);

        service.runAutomaticDeletion(TODAY);

        verifyNoInteractions(projectRepository, monitoringTableRecordSolarRepository, notificationService);
    }

    @Test
    @DisplayName("the advance notice goes out exactly advanceNoticeDays before the deletion date, without deleting")
    void advanceNoticeIsSentSevenDaysBefore() {
        givenFinalizedProjects(projectDueOn(1, "PROJPUBLIC1", TODAY.plusDays(7)));

        service.runAutomaticDeletion(TODAY);

        verify(notificationService).createNotificationForAllOperatingUsers(
                eq(ADVANCE_SUBJECT), contains("PROJPUBLIC1"));
        verify(notificationService).sendEmailToAllOperatingUsers(
                eq(ADVANCE_SUBJECT), contains(TODAY.plusDays(7).toString()));
        verifyNoInteractions(monitoringTableRecordSolarRepository);
    }

    @Test
    @DisplayName("no notice and no deletion on the days in between")
    void nothingHappensOnOtherDays() {
        givenFinalizedProjects(projectDueOn(1, "PROJPUBLIC1", TODAY.plusDays(3)));

        service.runAutomaticDeletion(TODAY);

        verifyNoInteractions(notificationService, monitoringTableRecordSolarRepository);
    }

    @Test
    @DisplayName("on the deletion date: start notice, every record deleted and logged, then the completion notice")
    void deletesEverythingOnTheDeletionDate() {
        Project project = projectDueOn(1, "PROJPUBLIC1", TODAY);
        givenFinalizedProjects(project);
        givenLinkedSites(project, site(10, "SITEPUBLIC1"));
        when(monitoringTableRecordSolarRepository.findPagesByPk(PK, 25))
                .thenReturn(List.of(List.of(record("2020-01-01"), record(null))));
        when(monitoringDataDeletionHistoryRepository.countByProjectIdAndResult(1, DeletionResult.SUCCESS))
                .thenReturn(2L);

        service.runAutomaticDeletion(TODAY);

        verify(notificationService).createNotificationForAllOperatingUsers(
                eq(STARTED_SUBJECT), contains("PROJPUBLIC1"));

        List<MonitoringDataDeletionHistory> history = savedHistory();
        assertEquals(2, history.size());
        for (MonitoringDataDeletionHistory row : history) {
            assertEquals(DeletionResult.SUCCESS, row.getResult());
            assertNull(row.getFailureReason());
            assertEquals(1, row.getProjectId());
            assertEquals(10, row.getSiteId());
        }

        verify(notificationService).createNotificationForAllOperatingUsers(
                eq(COMPLETED_SUBJECT), contains("削除件数: 2件"));
        verify(notificationService).sendEmailToAllOperatingUsers(
                eq(COMPLETED_SUBJECT), contains("PROJPUBLIC1"));
    }

    @Test
    @DisplayName("each page of a partition is deleted as its own batch")
    void everyPageIsDeleted() {
        Project project = projectDueOn(1, "PROJPUBLIC1", TODAY);
        givenFinalizedProjects(project);
        givenLinkedSites(project, site(10, "SITEPUBLIC1"));
        when(monitoringTableRecordSolarRepository.findPagesByPk(PK, 25))
                .thenReturn(List.of(List.of(record("2020-01-01")), List.of(record("2020-01-02"))));

        service.runAutomaticDeletion(TODAY);

        verify(monitoringTableRecordSolarRepository, times(2)).deleteBatch(any());
        assertEquals(2, savedHistory().size());
    }

    @Test
    @DisplayName("every site ever linked to the project is walked, using sitePublicId#projectPublicId")
    void walksEveryLinkedSite() {
        Project project = projectDueOn(1, "PROJPUBLIC1", TODAY.minusDays(30));
        givenFinalizedProjects(project);
        givenLinkedSites(project, site(10, "SITEPUBLIC1"), site(11, "SITEPUBLIC2"));

        service.runAutomaticDeletion(TODAY);

        verify(monitoringTableRecordSolarRepository).findPagesByPk("SITEPUBLIC1#PROJPUBLIC1", 25);
        verify(monitoringTableRecordSolarRepository).findPagesByPk("SITEPUBLIC2#PROJPUBLIC1", 25);
    }

    @Test
    @DisplayName("a project that is already empty is not reported as completed again")
    void emptyProjectSendsNoCompletionNotice() {
        Project project = projectDueOn(1, "PROJPUBLIC1", TODAY.minusDays(30));
        givenFinalizedProjects(project);
        givenLinkedSites(project, site(10, "SITEPUBLIC1"));
        when(monitoringTableRecordSolarRepository.findPagesByPk(PK, 25)).thenReturn(List.of());

        service.runAutomaticDeletion(TODAY);

        verify(monitoringTableRecordSolarRepository, never()).deleteBatch(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("items returned as unprocessed are retried until they are deleted")
    void unprocessedItemsAreRetried() {
        Project project = projectDueOn(1, "PROJPUBLIC1", TODAY.minusDays(1));
        givenFinalizedProjects(project);
        givenLinkedSites(project, site(10, "SITEPUBLIC1"));
        EnR002MonitoringRecord stubborn = record("2020-01-02");
        when(monitoringTableRecordSolarRepository.findPagesByPk(PK, 25))
                .thenReturn(List.of(List.of(record("2020-01-01"), stubborn)));
        when(monitoringTableRecordSolarRepository.deleteBatch(any()))
                .thenReturn(List.of(keyOf(stubborn)))
                .thenReturn(List.of());

        service.runAutomaticDeletion(TODAY);

        verify(monitoringTableRecordSolarRepository, times(2)).deleteBatch(any());
        assertTrue(savedHistory().stream().allMatch(row -> row.getResult() == DeletionResult.SUCCESS));
        verify(notificationService).createNotificationForAllOperatingUsers(eq(COMPLETED_SUBJECT), anyString());
    }

    @Test
    @DisplayName("items still unprocessed after every retry are logged as FAILURE and completion is not reported")
    void persistentFailureIsRecordedAndLeftForTheNextRun() {
        Project project = projectDueOn(1, "PROJPUBLIC1", TODAY.minusDays(1));
        givenFinalizedProjects(project);
        givenLinkedSites(project, site(10, "SITEPUBLIC1"));
        EnR002MonitoringRecord stubborn = record("2020-01-02");
        when(monitoringTableRecordSolarRepository.findPagesByPk(PK, 25))
                .thenReturn(List.of(List.of(record("2020-01-01"), stubborn), List.of(record("2020-01-03"))));
        when(monitoringTableRecordSolarRepository.deleteBatch(any())).thenReturn(List.of(keyOf(stubborn)));

        service.runAutomaticDeletion(TODAY);

        // First call plus five retries, and the following page is left for the next run.
        verify(monitoringTableRecordSolarRepository, times(6)).deleteBatch(any());
        List<MonitoringDataDeletionHistory> history = savedHistory();
        assertEquals(2, history.size());
        assertEquals(1, history.stream().filter(row -> row.getResult() == DeletionResult.SUCCESS).count());
        MonitoringDataDeletionHistory failure = history.stream()
                .filter(row -> row.getResult() == DeletionResult.FAILURE)
                .findFirst()
                .orElseThrow();
        assertEquals(stubborn.getTimeUpdated(), failure.getRecordTimeUpdated());
        assertEquals("DynamoDB throttled/unprocessed", failure.getFailureReason());
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("a DynamoDB error on one project is logged as FAILURE and does not stop the other projects")
    void errorOnOneProjectDoesNotStopTheOthers() {
        Project broken = projectDueOn(1, "PROJPUBLIC1", TODAY.minusDays(1));
        Project healthy = projectDueOn(2, "PROJPUBLIC2", TODAY.minusDays(1));
        givenFinalizedProjects(broken, healthy);
        givenLinkedSites(broken, site(10, "SITEPUBLIC1"));
        givenLinkedSites(healthy, site(20, "SITEPUBLIC2"));
        EnR002MonitoringRecord brokenRecord = record("2020-01-01");
        EnR002MonitoringRecord healthyRecord = record("2020-01-01");
        healthyRecord.setId("SITEPUBLIC2#PROJPUBLIC2");
        when(monitoringTableRecordSolarRepository.findPagesByPk(PK, 25))
                .thenReturn(List.of(List.of(brokenRecord)));
        when(monitoringTableRecordSolarRepository.findPagesByPk("SITEPUBLIC2#PROJPUBLIC2", 25))
                .thenReturn(List.of(List.of(healthyRecord)));
        when(monitoringTableRecordSolarRepository.deleteBatch(List.of(keyOf(brokenRecord))))
                .thenThrow(new IllegalStateException("table unavailable"));

        service.runAutomaticDeletion(TODAY);

        List<MonitoringDataDeletionHistory> history = savedHistory();
        assertEquals(2, history.size());
        MonitoringDataDeletionHistory failure = history.stream()
                .filter(row -> row.getProjectId() == 1)
                .findFirst()
                .orElseThrow();
        assertEquals(DeletionResult.FAILURE, failure.getResult());
        assertEquals("table unavailable", failure.getFailureReason());
        verify(notificationService, times(1))
                .createNotificationForAllOperatingUsers(eq(COMPLETED_SUBJECT), contains("PROJPUBLIC2"));
    }

    @Test
    @DisplayName("a project already past retention at go-live waits for the earliest deletion date and still gets its notices")
    void earliestDeletionDateProtectsTheBacklog() {
        Project project = projectDueOn(1, "PROJPUBLIC1", TODAY.minusYears(1));
        givenFinalizedProjects(project);
        when(retentionProperties.resolveEarliestDeletionDate()).thenReturn(TODAY.plusDays(7));

        service.runAutomaticDeletion(TODAY);

        verify(notificationService).createNotificationForAllOperatingUsers(
                eq(ADVANCE_SUBJECT), contains("PROJPUBLIC1"));
        verifyNoInteractions(monitoringTableRecordSolarRepository);
    }

    @Test
    @DisplayName("a failing notification does not stop the deletion")
    void notificationFailureDoesNotStopDeletion() {
        Project project = projectDueOn(1, "PROJPUBLIC1", TODAY);
        givenFinalizedProjects(project);
        givenLinkedSites(project, site(10, "SITEPUBLIC1"));
        when(monitoringTableRecordSolarRepository.findPagesByPk(PK, 25))
                .thenReturn(List.of(List.of(record("2020-01-01"))));
        org.mockito.Mockito.doThrow(new IllegalStateException("mail down"))
                .when(notificationService).sendEmailToAllOperatingUsers(anyString(), anyString());

        service.runAutomaticDeletion(TODAY);

        verify(monitoringTableRecordSolarRepository).deleteBatch(any());
        assertEquals(1, savedHistory().size());
    }

    @Test
    @DisplayName("the schedule lists only projects whose deletion date has not passed, soonest first")
    void scheduleListsUpcomingProjectsInOrder() {
        LocalDate today = LocalDate.now();
        Project later = projectDueOn(1, "PROJPUBLIC1", today.plusDays(30));
        Project sooner = projectDueOn(2, "PROJPUBLIC2", today.plusDays(5));
        Project past = projectDueOn(3, "PROJPUBLIC3", today.minusDays(5));
        Project neverFinalized = projectDueOn(4, "PROJPUBLIC4", today);
        neverFinalized.setStatusFinalizedAt(null);
        givenFinalizedProjects(later, sooner, past, neverFinalized);
        when(retentionProperties.isAutoDeletionEnabled()).thenReturn(true);

        MonitoringDeletionScheduleListResponse response = service.findDeletionSchedule();

        assertTrue(response.isAutoDeletionEnabled());
        assertEquals(2, response.getTotalProjects());
        assertEquals("PROJPUBLIC2", response.getSchedule().get(0).getProjectPublicId());
        assertEquals(5, response.getSchedule().get(0).getDaysUntilDeletion());
        assertEquals(today.plusDays(5), response.getSchedule().get(0).getScheduledDeletionDate());
        assertEquals("PROJPUBLIC1", response.getSchedule().get(1).getProjectPublicId());
    }

    @Test
    @DisplayName("findDeletionHistory delegates to the repository with a page/limit pageable")
    void findDeletionHistoryDelegatesToRepository() {
        MonitoringDataDeletionHistory history = MonitoringDataDeletionHistory.builder()
                .monitoringRecordId(PK)
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

    private void givenFinalizedProjects(Project... projects) {
        when(projectRepository.findByProjectStatus(FINALIZED_STATUS)).thenReturn(List.of(projects));
    }

    private void givenLinkedSites(Project project, LinkedSite... sites) {
        when(siteProjectRelationRepository.findLinkedSitesIncludingDeleted(project.getId()))
                .thenReturn(List.of(sites));
    }

    /** A finalized project whose two-year retention ends on the given date. */
    private Project projectDueOn(Integer id, String publicId, LocalDate deletionDate) {
        Project project = new Project();
        project.setId(id);
        project.setPublicId(publicId);
        project.setProjectName("project-" + id);
        project.setProjectStatus(FINALIZED_STATUS);
        project.setStatusFinalizedAt(deletionDate.minusYears(2).atTime(9, 0));
        return project;
    }

    private LinkedSite site(Integer id, String publicId) {
        return new LinkedSite() {
            @Override
            public Integer getSiteId() {
                return id;
            }

            @Override
            public String getSitePublicId() {
                return publicId;
            }
        };
    }

    private int recordSequence = 0;

    private EnR002MonitoringRecord record(String dateRecorded) {
        EnR002MonitoringRecord record = new EnR002MonitoringRecord();
        record.setId(PK);
        record.setTimeUpdated("2020-01-01T00:00:0" + (recordSequence++) + "Z");
        record.setDateRecorded(dateRecorded);
        return record;
    }

    private Key keyOf(EnR002MonitoringRecord record) {
        return Key.builder().partitionValue(record.getId()).sortValue(record.getTimeUpdated()).build();
    }

    @SuppressWarnings("unchecked")
    private List<MonitoringDataDeletionHistory> savedHistory() {
        ArgumentCaptor<List<MonitoringDataDeletionHistory>> captor = ArgumentCaptor.forClass(List.class);
        verify(monitoringDataDeletionHistoryRepository, org.mockito.Mockito.atLeastOnce()).saveAll(captor.capture());
        List<MonitoringDataDeletionHistory> rows = new ArrayList<>();
        captor.getAllValues().forEach(rows::addAll);
        return rows;
    }
}
