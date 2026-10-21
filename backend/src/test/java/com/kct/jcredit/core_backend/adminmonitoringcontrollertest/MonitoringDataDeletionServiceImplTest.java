package com.kct.jcredit.core_backend.adminmonitoringcontrollertest;

// Scope note: MonitoringDataDeletionServiceImpl - the three public methods:
//   findDeletionSchedule()                      -> blocks 01
//   runAutomaticDeletion(today)                 -> blocks 02 (timing / notices) and 03 (deleting)
//   findDeletionHistory(id, page, limit, user)  -> block 04
//
// NOT covered here:
//  - pauseBeforeRetry timing: retryBackoffMillis is 0 in every test so the suite stays fast.
//  - id, deletedAt and deletedBy of the history response: they come from JPA auditing columns
//    that a plain Mockito test cannot populate meaningfully.
//  - @Transactional(readOnly = true) on the two read methods.
//
// Strict stubs are on (no class-level LENIENT). Shared fixtures use lenient() because not every
// test touches every property; per-test stubs are strict so an unused one fails the test.
// Deletion failures are simulated with thenAnswer on deleteBatch(any()) rather than with
// argument-specific stubs, which strict mode would reject as potential stubbing problems.
//
// IMPORTS: copy the project-specific imports (entities, repositories, DTOs, ResponseConstants,
// DeletionResult, NotificationMessages, LinkedSite, ...) from MonitoringDataDeletionServiceImpl.
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.kct.jcredit.core_backend.config.MonitoringDataRetentionProperties;
import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.response.MonitoringDataDeletionHistoryListResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringDeletionScheduleListResponse;
import com.kct.jcredit.core_backend.dto.response.details.MonitoringDataDeletionHistoryDetailResponse;
import com.kct.jcredit.core_backend.dto.response.details.MonitoringDeletionScheduleDetailResponse;
import com.kct.jcredit.core_backend.entity.EnR002MonitoringRecord;
import com.kct.jcredit.core_backend.entity.MonitoringDataDeletionHistory;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.enums.DeletionResult;
import com.kct.jcredit.core_backend.repository.dynamodb.MonitoringTableRecordSolarRepository;
import com.kct.jcredit.core_backend.repository.postgres.MonitoringDataDeletionHistoryRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository.LinkedSite;
import com.kct.jcredit.core_backend.service.NotificationMessages;
import com.kct.jcredit.core_backend.service.NotificationService;
import com.kct.jcredit.core_backend.service.ProjectService;
import com.kct.jcredit.core_backend.service.impl.MonitoringDataDeletionServiceImpl;

import software.amazon.awssdk.enhanced.dynamodb.Key;

@ExtendWith(MockitoExtension.class)
@DisplayName("MonitoringDataDeletionServiceImpl のテスト")
class MonitoringDataDeletionServiceImplTest {

    private static final String FINALIZED_STATUS = "終了";
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 1);
    private static final String PK = "SITEPUBLIC1#PROJPUBLIC1";
    private static final int BATCH_SIZE = 25;
    private static final String USER_ID = "operator01";

    private static final String ADVANCE_SUBJECT = "モニタリングデータ削除予定のお知らせ";
    private static final String STARTED_SUBJECT = "モニタリングデータ削除開始のお知らせ";
    private static final String COMPLETED_SUBJECT = "モニタリングデータ削除完了のお知らせ";

    @Mock private ProjectRepository projectRepository;
    @Mock private SiteProjectRelationRepository siteProjectRelationRepository;
    @Mock private MonitoringTableRecordSolarRepository monitoringTableRecordSolarRepository;
    @Mock private MonitoringDataDeletionHistoryRepository monitoringDataDeletionHistoryRepository;
    @Mock private MonitoringDataRetentionProperties retentionProperties;
    @Mock private NotificationService notificationService;
    @Mock private ProjectService projectService;

    // Real instance, so the tests also prove the message keys exist in mail/messages.properties.
    @Spy
    private NotificationMessages messages = new NotificationMessages();

    @InjectMocks
    private MonitoringDataDeletionServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(retentionProperties.getRetentionYears()).thenReturn(2);
        lenient().when(retentionProperties.getAdvanceNoticeDays()).thenReturn(7);
        lenient().when(retentionProperties.getRetryBackoffMillis()).thenReturn(0L);
        lenient().when(retentionProperties.resolveEarliestDeletionDate())
                .thenReturn(LocalDate.of(2000, 1, 1));
    }

    // ==================================================================
    // 01. findDeletionSchedule
    // ==================================================================

    @Test
    @DisplayName("UT-AMS-01-01: 正常 - 予定一覧にプロジェクト情報・削除予定日・残日数が設定される")
    void findDeletionSchedule_valid_returnsProjectDetails() {
        LocalDate today = LocalDate.now();
        Project project = projectDueOn(1, "PROJPUBLIC1", today.plusDays(30));
        givenFinalizedProjects(project);
        when(retentionProperties.isAutoDeletionEnabled()).thenReturn(true);

        MonitoringDeletionScheduleListResponse response = service.findDeletionSchedule();

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getMessage())
                .isEqualTo(ResponseConstants.MSG_MONITORING_DELETION_SCHEDULE_RETRIEVED);
        assertThat(response.isAutoDeletionEnabled()).isTrue();
        assertThat(response.getTotalProjects()).isEqualTo(1);

        MonitoringDeletionScheduleDetailResponse item = response.getSchedule().get(0);
        assertThat(item.getProjectId()).isEqualTo(1);
        assertThat(item.getProjectPublicId()).isEqualTo("PROJPUBLIC1");
        assertThat(item.getProjectName()).isEqualTo("project-1");
        assertThat(item.getStatusFinalizedAt()).isEqualTo(project.getStatusFinalizedAt());
        assertThat(item.getScheduledDeletionDate()).isEqualTo(today.plusDays(30));
        assertThat(item.getDaysUntilDeletion()).isEqualTo(30);
    }

    @Test
    @DisplayName("UT-AMS-01-02: 正常 - 削除予定日の昇順で並び、同じ日はprojectIdの昇順になる")
    void findDeletionSchedule_sortedByDateThenProjectId() {
        LocalDate today = LocalDate.now();
        givenFinalizedProjects(
                projectDueOn(1, "PROJPUBLIC1", today.plusDays(30)),
                projectDueOn(9, "PROJPUBLIC9", today.plusDays(10)),
                projectDueOn(2, "PROJPUBLIC2", today.plusDays(5)),
                projectDueOn(3, "PROJPUBLIC3", today.plusDays(10)));

        MonitoringDeletionScheduleListResponse response = service.findDeletionSchedule();

        assertThat(response.getSchedule())
                .extracting(MonitoringDeletionScheduleDetailResponse::getProjectPublicId)
                .containsExactly("PROJPUBLIC2", "PROJPUBLIC3", "PROJPUBLIC9", "PROJPUBLIC1");
    }

    @Test
    @DisplayName("UT-AMS-01-03: 正常 - 保存期間（年）は設定値が使われる")
    void findDeletionSchedule_usesConfiguredRetentionYears() {
        LocalDate today = LocalDate.now();
        LocalDate finalizedOn = today.minusYears(1);
        when(retentionProperties.getRetentionYears()).thenReturn(5);
        givenFinalizedProjects(projectFinalizedOn(1, "PROJPUBLIC1", finalizedOn));

        MonitoringDeletionScheduleListResponse response = service.findDeletionSchedule();

        assertThat(response.getSchedule().get(0).getScheduledDeletionDate())
                .isEqualTo(finalizedOn.plusYears(5));
    }

    @Test
    @DisplayName("UT-AMS-01-04: 正常 - 最早削除日が保存期間満了日より後の場合、最早削除日が削除予定日になる")
    void findDeletionSchedule_earliestDeletionDateWinsWhenLater() {
        LocalDate today = LocalDate.now();
        givenFinalizedProjects(projectDueOn(1, "PROJPUBLIC1", today.plusDays(10)));
        when(retentionProperties.resolveEarliestDeletionDate()).thenReturn(today.plusDays(40));

        MonitoringDeletionScheduleListResponse response = service.findDeletionSchedule();

        MonitoringDeletionScheduleDetailResponse item = response.getSchedule().get(0);
        assertThat(item.getScheduledDeletionDate()).isEqualTo(today.plusDays(40));
        assertThat(item.getDaysUntilDeletion()).isEqualTo(40);
    }

    @Test
    @DisplayName("UT-AMS-01-05: 準正常 - 終了日時が未設定のプロジェクトは一覧に含まれない")
    void findDeletionSchedule_projectWithoutFinalizedAtIsSkipped() {
        LocalDate today = LocalDate.now();
        Project neverFinalized = projectDueOn(4, "PROJPUBLIC4", today.plusDays(30));
        neverFinalized.setStatusFinalizedAt(null);
        givenFinalizedProjects(projectDueOn(1, "PROJPUBLIC1", today.plusDays(30)), neverFinalized);

        MonitoringDeletionScheduleListResponse response = service.findDeletionSchedule();

        assertThat(response.getSchedule())
                .extracting(MonitoringDeletionScheduleDetailResponse::getProjectPublicId)
                .containsExactly("PROJPUBLIC1");
    }

    @Test
    @DisplayName("UT-AMS-01-06: 正常 - 対象が0件でもOKと空の一覧を返し、自動削除フラグはそのまま反映される")
    void findDeletionSchedule_noProjects_returnsEmptySchedule() {
        givenFinalizedProjects();
        when(retentionProperties.isAutoDeletionEnabled()).thenReturn(false);

        MonitoringDeletionScheduleListResponse response = service.findDeletionSchedule();

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getTotalProjects()).isEqualTo(0);
        assertThat(response.getSchedule()).isEmpty();
        assertThat(response.isAutoDeletionEnabled()).isFalse();
    }

    @Test
    @DisplayName("UT-AMS-01-07: 要確認 - 削除予定日を過ぎたプロジェクトは一覧から消える")
    void findDeletionSchedule_pastDeletionDateIsDropped() {
        // NOTE: a project whose date has passed disappears from the schedule whether its data
        // was deleted or the deletion is stuck (runAutomaticDeletion leaves it for the next
        // run on failure). The screen cannot show a stalled deletion. Whether to list such
        // projects as overdue is a design decision; recorded as current behaviour.
        LocalDate today = LocalDate.now();
        givenFinalizedProjects(
                projectDueOn(1, "PROJPUBLIC1", today.plusDays(5)),
                projectDueOn(3, "PROJPUBLIC3", today.minusDays(5)));

        MonitoringDeletionScheduleListResponse response = service.findDeletionSchedule();

        assertThat(response.getSchedule())
                .extracting(MonitoringDeletionScheduleDetailResponse::getProjectPublicId)
                .containsExactly("PROJPUBLIC1");
    }

    @Test
    @DisplayName("UT-AMS-01-08: 異常 - 最早削除日が未設定の場合、ジョブは実行されないため自動削除を「有効」と表示してはならない【要修正】")
    void findDeletionSchedule_earliestDateNotConfigured_shouldNotReportAutoDeletionEnabled() {
        // Pattern B: runAutomaticDeletion refuses to run while the earliest deletion date is
        // empty, but the schedule copies the master switch as it is.
        // The flag must describe whether the job will really run.
        LocalDate today = LocalDate.now();
        when(retentionProperties.resolveEarliestDeletionDate()).thenReturn(null);
        when(retentionProperties.isAutoDeletionEnabled()).thenReturn(true);
        givenFinalizedProjects(projectDueOn(1, "PROJPUBLIC1", today.plusDays(30)));

        MonitoringDeletionScheduleListResponse response = service.findDeletionSchedule();

        assertThat(response.isAutoDeletionEnabled())
                .as("最早削除日が未設定の間はジョブが実行を拒否するため、有効と表示してはならない")
                .isFalse();
    }

    // ==================================================================
    // 02. runAutomaticDeletion - timing and notices
    // ==================================================================

    @Test
    @DisplayName("UT-AMS-02-01: 準正常 - 最早削除日が未設定の場合、何も実行しない")
    void runAutomaticDeletion_earliestDateNotConfigured_doesNothing() {
        when(retentionProperties.resolveEarliestDeletionDate()).thenReturn(null);

        service.runAutomaticDeletion(TODAY);

        verifyNoInteractions(projectRepository, siteProjectRelationRepository,
                monitoringTableRecordSolarRepository, monitoringDataDeletionHistoryRepository,
                notificationService);
    }

    @Test
    @DisplayName("UT-AMS-02-02: 正常 - 事前通知は削除日のadvanceNoticeDays日前に送られ、削除は行わない")
    void runAutomaticDeletion_advanceNoticeSentSevenDaysBefore() {
        givenFinalizedProjects(projectDueOn(1, "PROJPUBLIC1", TODAY.plusDays(7)));

        service.runAutomaticDeletion(TODAY);

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(notificationService).createNotificationForAllOperatingUsers(eq(ADVANCE_SUBJECT), body.capture());
        assertThat(body.getValue()).contains("PROJPUBLIC1").contains(TODAY.plusDays(7).toString());
        verify(notificationService).sendEmailToAllOperatingUsers(eq(ADVANCE_SUBJECT), anyString());
        verifyNoInteractions(siteProjectRelationRepository, monitoringTableRecordSolarRepository);
    }

    @Test
    @DisplayName("UT-AMS-02-03: 正常 - 事前通知の日数は設定値に従う")
    void runAutomaticDeletion_advanceNoticeFollowsConfiguredDays() {
        when(retentionProperties.getAdvanceNoticeDays()).thenReturn(14);
        givenFinalizedProjects(
                projectDueOn(1, "PROJPUBLIC1", TODAY.plusDays(14)),
                projectDueOn(2, "PROJPUBLIC2", TODAY.plusDays(7)));

        service.runAutomaticDeletion(TODAY);

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(notificationService).createNotificationForAllOperatingUsers(eq(ADVANCE_SUBJECT), body.capture());
        assertThat(body.getValue()).contains("PROJPUBLIC1").doesNotContain("PROJPUBLIC2");
    }

    @Test
    @DisplayName("UT-AMS-02-04: 正常 - 通知日・削除日以外の日は通知も削除も行わない")
    void runAutomaticDeletion_otherDays_nothingHappens() {
        givenFinalizedProjects(projectDueOn(1, "PROJPUBLIC1", TODAY.plusDays(3)));

        service.runAutomaticDeletion(TODAY);

        verifyNoInteractions(notificationService, siteProjectRelationRepository,
                monitoringTableRecordSolarRepository);
    }

    @Test
    @DisplayName("UT-AMS-02-05: 正常 - 削除日当日は開始通知を送ってから削除を実行する")
    void runAutomaticDeletion_onDeletionDate_notifiesThenDeletes() {
        Project project = projectDueOn(1, "PROJPUBLIC1", TODAY);
        givenFinalizedProjects(project);
        givenLinkedSites(project, site(10, "SITEPUBLIC1"));
        when(monitoringTableRecordSolarRepository.findPagesByPk(PK, BATCH_SIZE))
                .thenReturn(List.of(List.of(record("2020-01-01"))));

        service.runAutomaticDeletion(TODAY);

        InOrder order = inOrder(notificationService, monitoringTableRecordSolarRepository);
        order.verify(notificationService).createNotificationForAllOperatingUsers(
                eq(STARTED_SUBJECT), contains("PROJPUBLIC1"));
        order.verify(monitoringTableRecordSolarRepository).deleteBatch(any());
    }

    @Test
    @DisplayName("UT-AMS-02-06: 正常 - 削除日を過ぎたプロジェクトも削除対象になるが、開始通知は送られない")
    void runAutomaticDeletion_overdueProject_isDeletedWithoutStartedNotice() {
        Project project = projectDueOn(1, "PROJPUBLIC1", TODAY.minusDays(30));
        givenFinalizedProjects(project);
        givenLinkedSites(project, site(10, "SITEPUBLIC1"));
        when(monitoringTableRecordSolarRepository.findPagesByPk(PK, BATCH_SIZE))
                .thenReturn(List.of(List.of(record("2020-01-01"))));

        service.runAutomaticDeletion(TODAY);

        verify(monitoringTableRecordSolarRepository).deleteBatch(any());
        verify(notificationService, never())
                .createNotificationForAllOperatingUsers(eq(STARTED_SUBJECT), anyString());
    }

    @Test
    @DisplayName("UT-AMS-02-07: 正常 - 同じ日に通知対象が複数ある場合、通知は1通にまとめられる")
    void runAutomaticDeletion_multipleProjectsShareOneNotice() {
        givenFinalizedProjects(
                projectDueOn(1, "PROJPUBLIC1", TODAY.plusDays(7)),
                projectDueOn(2, "PROJPUBLIC2", TODAY.plusDays(7)));

        service.runAutomaticDeletion(TODAY);

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(notificationService, times(1))
                .createNotificationForAllOperatingUsers(eq(ADVANCE_SUBJECT), body.capture());
        assertThat(body.getValue()).contains("PROJPUBLIC1").contains("PROJPUBLIC2");
    }

    @Test
    @DisplayName("UT-AMS-02-08: 正常 - 通知はアプリ内とメールの両方に同じ件名・本文で送られる")
    void runAutomaticDeletion_sameContentOnBothChannels() {
        givenFinalizedProjects(projectDueOn(1, "PROJPUBLIC1", TODAY.plusDays(7)));

        service.runAutomaticDeletion(TODAY);

        ArgumentCaptor<String> inAppSubject = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> inAppBody = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> mailSubject = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> mailBody = ArgumentCaptor.forClass(String.class);
        verify(notificationService).createNotificationForAllOperatingUsers(
                inAppSubject.capture(), inAppBody.capture());
        verify(notificationService).sendEmailToAllOperatingUsers(
                mailSubject.capture(), mailBody.capture());
        assertThat(mailSubject.getValue()).isEqualTo(inAppSubject.getValue());
        assertThat(mailBody.getValue()).isEqualTo(inAppBody.getValue());
    }

    @Test
    @DisplayName("UT-AMS-02-09: 正常 - 保存期間を過ぎた既存プロジェクトは最早削除日まで削除されず、事前通知は届く")
    void runAutomaticDeletion_earliestDateProtectsBacklog() {
        givenFinalizedProjects(projectDueOn(1, "PROJPUBLIC1", TODAY.minusYears(1)));
        when(retentionProperties.resolveEarliestDeletionDate()).thenReturn(TODAY.plusDays(7));

        service.runAutomaticDeletion(TODAY);

        verify(notificationService).createNotificationForAllOperatingUsers(
                eq(ADVANCE_SUBJECT), contains("PROJPUBLIC1"));
        verifyNoInteractions(siteProjectRelationRepository, monitoringTableRecordSolarRepository);
    }

    @Test
    @DisplayName("UT-AMS-02-10: 準正常 - アプリ内通知が失敗してもメール送信と削除は続行される")
    void runAutomaticDeletion_inAppNotificationFailure_doesNotStopMailOrDeletion() {
        Project project = projectDueOn(1, "PROJPUBLIC1", TODAY);
        givenFinalizedProjects(project);
        givenLinkedSites(project, site(10, "SITEPUBLIC1"));
        when(monitoringTableRecordSolarRepository.findPagesByPk(PK, BATCH_SIZE))
                .thenReturn(List.of(List.of(record("2020-01-01"))));
        doThrow(new IllegalStateException("in-app down"))
                .when(notificationService).createNotificationForAllOperatingUsers(anyString(), anyString());

        service.runAutomaticDeletion(TODAY);

        verify(notificationService).sendEmailToAllOperatingUsers(eq(STARTED_SUBJECT), anyString());
        verify(monitoringTableRecordSolarRepository).deleteBatch(any());
    }

    @Test
    @DisplayName("UT-AMS-02-11: 準正常 - メール通知が失敗しても削除は続行される")
    void runAutomaticDeletion_mailFailure_doesNotStopDeletion() {
        Project project = projectDueOn(1, "PROJPUBLIC1", TODAY);
        givenFinalizedProjects(project);
        givenLinkedSites(project, site(10, "SITEPUBLIC1"));
        when(monitoringTableRecordSolarRepository.findPagesByPk(PK, BATCH_SIZE))
                .thenReturn(List.of(List.of(record("2020-01-01"))));
        doThrow(new IllegalStateException("mail down"))
                .when(notificationService).sendEmailToAllOperatingUsers(anyString(), anyString());

        service.runAutomaticDeletion(TODAY);

        verify(monitoringTableRecordSolarRepository).deleteBatch(any());
        assertThat(savedHistory()).hasSize(1);
    }

    // ==================================================================
    // 03. runAutomaticDeletion - deleting and recording
    // ==================================================================

    @Test
    @DisplayName("UT-AMS-03-01: 正常 - 全レコードが削除されて履歴にSUCCESSで残り、完了通知が送られる")
    void runAutomaticDeletion_deletesEverythingAndLogsSuccess() {
        Project project = projectDueOn(1, "PROJPUBLIC1", TODAY.minusDays(1));
        givenFinalizedProjects(project);
        givenLinkedSites(project, site(10, "SITEPUBLIC1"));
        EnR002MonitoringRecord first = record("2020-01-01");
        EnR002MonitoringRecord second = record(null);
        when(monitoringTableRecordSolarRepository.findPagesByPk(PK, BATCH_SIZE))
                .thenReturn(List.of(List.of(first, second)));

        service.runAutomaticDeletion(TODAY);

        List<MonitoringDataDeletionHistory> history = savedHistory();
        assertThat(history).hasSize(2);
        assertThat(history).extracting(MonitoringDataDeletionHistory::getRecordTimeUpdated)
                .containsExactly(first.getTimeUpdated(), second.getTimeUpdated());
        assertThat(history).extracting(MonitoringDataDeletionHistory::getDateRecorded)
                .containsExactly("2020-01-01", null);
        for (MonitoringDataDeletionHistory row : history) {
            assertThat(row.getResult()).isEqualTo(DeletionResult.SUCCESS);
            assertThat(row.getFailureReason()).isNull();
            assertThat(row.getMonitoringRecordId()).isEqualTo(PK);
            assertThat(row.getProjectId()).isEqualTo(1);
            assertThat(row.getSiteId()).isEqualTo(10);
        }
        verify(notificationService).createNotificationForAllOperatingUsers(
                eq(COMPLETED_SUBJECT), contains("PROJPUBLIC1"));
        verify(notificationService).sendEmailToAllOperatingUsers(
                eq(COMPLETED_SUBJECT), contains("PROJPUBLIC1"));
    }

    @Test
    @DisplayName("UT-AMS-03-02: 正常 - ページごとに1バッチ（25件単位）として順に削除される")
    void runAutomaticDeletion_eachPageIsOneBatchInOrder() {
        Project project = projectDueOn(1, "PROJPUBLIC1", TODAY.minusDays(1));
        givenFinalizedProjects(project);
        givenLinkedSites(project, site(10, "SITEPUBLIC1"));
        EnR002MonitoringRecord firstPage = record("2020-01-01");
        EnR002MonitoringRecord secondPage = record("2020-01-02");
        when(monitoringTableRecordSolarRepository.findPagesByPk(PK, BATCH_SIZE))
                .thenReturn(List.of(List.of(firstPage), List.of(secondPage)));

        service.runAutomaticDeletion(TODAY);

        InOrder order = inOrder(monitoringTableRecordSolarRepository);
        order.verify(monitoringTableRecordSolarRepository).deleteBatch(List.of(keyOf(firstPage)));
        order.verify(monitoringTableRecordSolarRepository).deleteBatch(List.of(keyOf(secondPage)));
        assertThat(savedHistory()).hasSize(2);
    }

    @Test
    @DisplayName("UT-AMS-03-03: 正常 - 紐づく全サイト（削除済みを含む）を「サイト公開ID#プロジェクト公開ID」で走査する")
    void runAutomaticDeletion_walksEveryLinkedSite() {
        Project project = projectDueOn(1, "PROJPUBLIC1", TODAY.minusDays(30));
        givenFinalizedProjects(project);
        givenLinkedSites(project, site(10, "SITEPUBLIC1"), site(11, "SITEPUBLIC2"));

        service.runAutomaticDeletion(TODAY);

        verify(monitoringTableRecordSolarRepository).findPagesByPk("SITEPUBLIC1#PROJPUBLIC1", BATCH_SIZE);
        verify(monitoringTableRecordSolarRepository).findPagesByPk("SITEPUBLIC2#PROJPUBLIC1", BATCH_SIZE);
    }

    @Test
    @DisplayName("UT-AMS-03-04: 正常 - 既に空のプロジェクトは削除も履歴保存も完了通知も行わない")
    void runAutomaticDeletion_alreadyEmptyProject_isSilent() {
        Project project = projectDueOn(1, "PROJPUBLIC1", TODAY.minusDays(30));
        givenFinalizedProjects(project);
        givenLinkedSites(project, site(10, "SITEPUBLIC1"));
        when(monitoringTableRecordSolarRepository.findPagesByPk(PK, BATCH_SIZE)).thenReturn(List.of());

        service.runAutomaticDeletion(TODAY);

        verify(monitoringTableRecordSolarRepository, never()).deleteBatch(any());
        verify(monitoringDataDeletionHistoryRepository, never()).saveAll(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("UT-AMS-03-05: 正常 - 完了通知の削除件数は今回分ではなく、履歴上のSUCCESS累計")
    void runAutomaticDeletion_completionNoticeReportsTotalFromHistory() {
        // This run deletes one record; the history already holds five SUCCESS rows in total.
        Project project = projectDueOn(1, "PROJPUBLIC1", TODAY.minusDays(1));
        givenFinalizedProjects(project);
        givenLinkedSites(project, site(10, "SITEPUBLIC1"));
        when(monitoringTableRecordSolarRepository.findPagesByPk(PK, BATCH_SIZE))
                .thenReturn(List.of(List.of(record("2020-01-01"))));
        when(monitoringDataDeletionHistoryRepository.countByProjectIdAndResult(1, DeletionResult.SUCCESS))
                .thenReturn(5L);

        service.runAutomaticDeletion(TODAY);

        verify(notificationService).createNotificationForAllOperatingUsers(
                eq(COMPLETED_SUBJECT), contains("削除件数: 5件"));
    }

    @Test
    @DisplayName("UT-AMS-03-06: 正常 - 未処理分は残りのキーだけで再試行され、成功すれば全件SUCCESSになる")
    void runAutomaticDeletion_unprocessedItemsRetriedUntilDeleted() {
        Project project = projectDueOn(1, "PROJPUBLIC1", TODAY.minusDays(1));
        givenFinalizedProjects(project);
        givenLinkedSites(project, site(10, "SITEPUBLIC1"));
        EnR002MonitoringRecord done = record("2020-01-01");
        EnR002MonitoringRecord stubborn = record("2020-01-02");
        when(monitoringTableRecordSolarRepository.findPagesByPk(PK, BATCH_SIZE))
                .thenReturn(List.of(List.of(done, stubborn)));
        when(monitoringTableRecordSolarRepository.deleteBatch(any()))
                .thenReturn(List.of(keyOf(stubborn)))
                .thenReturn(List.of());

        service.runAutomaticDeletion(TODAY);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Key>> keys = ArgumentCaptor.forClass(List.class);
        verify(monitoringTableRecordSolarRepository, times(2)).deleteBatch(keys.capture());
        assertThat(keys.getAllValues().get(0)).containsExactly(keyOf(done), keyOf(stubborn));
        assertThat(keys.getAllValues().get(1)).containsExactly(keyOf(stubborn));
        assertThat(savedHistory()).allMatch(row -> row.getResult() == DeletionResult.SUCCESS);
        verify(notificationService).createNotificationForAllOperatingUsers(eq(COMPLETED_SUBJECT), anyString());
    }

    @Test
    @DisplayName("UT-AMS-03-07: 準正常 - 再試行を使い切ると履歴はFAILUREで、後続ページには進まず完了通知も送らない")
    void runAutomaticDeletion_retriesExhausted_logsFailureAndStops() {
        Project project = projectDueOn(1, "PROJPUBLIC1", TODAY.minusDays(1));
        givenFinalizedProjects(project);
        givenLinkedSites(project, site(10, "SITEPUBLIC1"));
        EnR002MonitoringRecord done = record("2020-01-01");
        EnR002MonitoringRecord stubborn = record("2020-01-02");
        EnR002MonitoringRecord nextPage = record("2020-01-03");
        when(monitoringTableRecordSolarRepository.findPagesByPk(PK, BATCH_SIZE))
                .thenReturn(List.of(List.of(done, stubborn), List.of(nextPage)));
        givenDeleteLeavesBehind(keyOf(stubborn));

        service.runAutomaticDeletion(TODAY);

        // First attempt plus five retries; the next page is left for the next run.
        verify(monitoringTableRecordSolarRepository, times(6)).deleteBatch(any());
        verify(monitoringTableRecordSolarRepository, never()).deleteBatch(List.of(keyOf(nextPage)));

        List<MonitoringDataDeletionHistory> history = savedHistory();
        assertThat(history).hasSize(2);
        MonitoringDataDeletionHistory failure = history.stream()
                .filter(row -> row.getResult() == DeletionResult.FAILURE)
                .findFirst()
                .orElseThrow();
        assertThat(failure.getRecordTimeUpdated()).isEqualTo(stubborn.getTimeUpdated());
        assertThat(failure.getFailureReason()).isEqualTo("DynamoDB throttled/unprocessed");
        assertThat(history.stream().filter(row -> row.getResult() == DeletionResult.SUCCESS)).hasSize(1);
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("UT-AMS-03-08: 準正常 - あるサイトで失敗しても他のサイトは処理されるが、完了通知は送られない")
    void runAutomaticDeletion_oneSiteFails_otherSiteStillProcessed() {
        Project project = projectDueOn(1, "PROJPUBLIC1", TODAY.minusDays(1));
        givenFinalizedProjects(project);
        givenLinkedSites(project, site(10, "SITEPUBLIC1"), site(11, "SITEPUBLIC2"));
        EnR002MonitoringRecord stuck = record("2020-01-01");
        EnR002MonitoringRecord fine = recordOf("SITEPUBLIC2#PROJPUBLIC1", "2020-01-02");
        when(monitoringTableRecordSolarRepository.findPagesByPk("SITEPUBLIC1#PROJPUBLIC1", BATCH_SIZE))
                .thenReturn(List.of(List.of(stuck)));
        when(monitoringTableRecordSolarRepository.findPagesByPk("SITEPUBLIC2#PROJPUBLIC1", BATCH_SIZE))
                .thenReturn(List.of(List.of(fine)));
        givenDeleteLeavesBehind(keyOf(stuck));

        service.runAutomaticDeletion(TODAY);

        List<MonitoringDataDeletionHistory> history = savedHistory();
        assertThat(history).hasSize(2);
        assertThat(history).filteredOn(row -> row.getSiteId().equals(10))
                .extracting(MonitoringDataDeletionHistory::getResult).containsExactly(DeletionResult.FAILURE);
        assertThat(history).filteredOn(row -> row.getSiteId().equals(11))
                .extracting(MonitoringDataDeletionHistory::getResult).containsExactly(DeletionResult.SUCCESS);
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("UT-AMS-03-09: 準正常 - DynamoDB例外はFAILUREとして記録され、他のプロジェクトの削除は続行される")
    void runAutomaticDeletion_errorOnOneProject_doesNotStopOthers() {
        Project broken = projectDueOn(1, "PROJPUBLIC1", TODAY.minusDays(1));
        Project healthy = projectDueOn(2, "PROJPUBLIC2", TODAY.minusDays(1));
        givenFinalizedProjects(broken, healthy);
        givenLinkedSites(broken, site(10, "SITEPUBLIC1"));
        givenLinkedSites(healthy, site(20, "SITEPUBLIC2"));
        EnR002MonitoringRecord brokenRecord = record("2020-01-01");
        EnR002MonitoringRecord healthyRecord = recordOf("SITEPUBLIC2#PROJPUBLIC2", "2020-01-01");
        when(monitoringTableRecordSolarRepository.findPagesByPk(PK, BATCH_SIZE))
                .thenReturn(List.of(List.of(brokenRecord)));
        when(monitoringTableRecordSolarRepository.findPagesByPk("SITEPUBLIC2#PROJPUBLIC2", BATCH_SIZE))
                .thenReturn(List.of(List.of(healthyRecord)));
        when(monitoringTableRecordSolarRepository.deleteBatch(any())).thenAnswer(invocation -> {
            List<Key> requested = invocation.getArgument(0);
            if (requested.contains(keyOf(brokenRecord))) {
                throw new IllegalStateException("table unavailable");
            }
            return List.of();
        });

        service.runAutomaticDeletion(TODAY);

        List<MonitoringDataDeletionHistory> history = savedHistory();
        assertThat(history).hasSize(2);
        MonitoringDataDeletionHistory failure = history.stream()
                .filter(row -> row.getProjectId().equals(1)).findFirst().orElseThrow();
        assertThat(failure.getResult()).isEqualTo(DeletionResult.FAILURE);
        assertThat(failure.getFailureReason()).isEqualTo("table unavailable");
        MonitoringDataDeletionHistory success = history.stream()
                .filter(row -> row.getProjectId().equals(2)).findFirst().orElseThrow();
        assertThat(success.getResult()).isEqualTo(DeletionResult.SUCCESS);

        verify(notificationService, times(1))
                .createNotificationForAllOperatingUsers(eq(COMPLETED_SUBJECT), contains("PROJPUBLIC2"));
        verify(notificationService, never())
                .createNotificationForAllOperatingUsers(eq(COMPLETED_SUBJECT), contains("PROJPUBLIC1"));
    }

    @Test
    @DisplayName("UT-AMS-03-10: 境界値 - 失敗理由は500文字で切り詰められる")
    void runAutomaticDeletion_failureReasonTruncatedTo500Characters() {
        Project project = projectDueOn(1, "PROJPUBLIC1", TODAY.minusDays(1));
        givenFinalizedProjects(project);
        givenLinkedSites(project, site(10, "SITEPUBLIC1"));
        when(monitoringTableRecordSolarRepository.findPagesByPk(PK, BATCH_SIZE))
                .thenReturn(List.of(List.of(record("2020-01-01"))));
        when(monitoringTableRecordSolarRepository.deleteBatch(any()))
                .thenThrow(new IllegalStateException("x".repeat(600)));

        service.runAutomaticDeletion(TODAY);

        assertThat(savedHistory().get(0).getFailureReason()).hasSize(500);
    }

    @Test
    @DisplayName("UT-AMS-03-11: 異常 - 例外メッセージがnullでも、失敗理由は「null」という文字列であってはならない【要修正】")
    void runAutomaticDeletion_exceptionWithoutMessage_shouldGiveUsableFailureReason() {
        // Pattern B: String.valueOf(e.getMessage()) turns a missing message into the literal text "null".
        Project project = projectDueOn(1, "PROJPUBLIC1", TODAY.minusDays(1));
        givenFinalizedProjects(project);
        givenLinkedSites(project, site(10, "SITEPUBLIC1"));
        when(monitoringTableRecordSolarRepository.findPagesByPk(PK, BATCH_SIZE))
                .thenReturn(List.of(List.of(record("2020-01-01"))));
        when(monitoringTableRecordSolarRepository.deleteBatch(any())).thenThrow(new IllegalStateException());

        service.runAutomaticDeletion(TODAY);

        assertThat(savedHistory().get(0).getFailureReason())
                .as("メッセージの無い例外でも、原因を読み取れる失敗理由が残るべき")
                .isNotBlank()
                .isNotEqualTo("null");
    }

    @Test
    @DisplayName("UT-AMS-03-12: 要確認 - パーティションの取得自体が失敗した場合、履歴は残らない")
    void runAutomaticDeletion_partitionReadFails_leavesNoHistory() {
        // NOTE: a failure while reading the partition is only logged. Nothing reaches
        // monitoring_data_deletion_history, so the history screen cannot show that this
        // project's deletion is failing. The run itself must not throw.
        Project project = projectDueOn(1, "PROJPUBLIC1", TODAY.minusDays(1));
        givenFinalizedProjects(project);
        givenLinkedSites(project, site(10, "SITEPUBLIC1"));
        when(monitoringTableRecordSolarRepository.findPagesByPk(PK, BATCH_SIZE))
                .thenThrow(new IllegalStateException("scan failed"));

        assertThatCode(() -> service.runAutomaticDeletion(TODAY)).doesNotThrowAnyException();

        verify(monitoringDataDeletionHistoryRepository, never()).saveAll(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("UT-AMS-03-13: 要確認 - 履歴の保存に失敗しても削除済みデータは戻らず、処理は続行し完了通知は送らない")
    void runAutomaticDeletion_historySaveFails_recordsAreAlreadyGone() {
        // NOTE: DynamoDB is deleted before the history row is written, and the two cannot
        // share a transaction. If saveAll fails the records are gone with no audit row, and
        // the next run finds nothing left to log. Recorded as current behaviour.
        Project project = projectDueOn(1, "PROJPUBLIC1", TODAY.minusDays(1));
        givenFinalizedProjects(project);
        givenLinkedSites(project, site(10, "SITEPUBLIC1"));
        when(monitoringTableRecordSolarRepository.findPagesByPk(PK, BATCH_SIZE))
                .thenReturn(List.of(List.of(record("2020-01-01"))));
        doThrow(new IllegalStateException("db down"))
                .when(monitoringDataDeletionHistoryRepository).saveAll(any());

        assertThatCode(() -> service.runAutomaticDeletion(TODAY)).doesNotThrowAnyException();

        verify(monitoringTableRecordSolarRepository).deleteBatch(any());
        verify(notificationService, never())
                .createNotificationForAllOperatingUsers(eq(COMPLETED_SUBJECT), anyString());
    }

    // ==================================================================
    // 04. findDeletionHistory
    // ==================================================================

    @Test
    @DisplayName("UT-AMS-04-01: 正常 - 閲覧可能なプロジェクトを指定すると、そのプロジェクトの履歴が返る")
    void findDeletionHistory_accessibleProject_returnsItsHistory() {
        givenAccessibleProject(1, "PUB-1");
        Page<MonitoringDataDeletionHistory> page = new PageImpl<>(List.of(historyOf(1)));
        when(monitoringDataDeletionHistoryRepository.findByProjectId(eq(1), any(Pageable.class)))
                .thenReturn(page);

        MonitoringDataDeletionHistoryListResponse response = service.findDeletionHistory(1, 1, 20, USER_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getMessage())
                .isEqualTo(ResponseConstants.MSG_MONITORING_DELETION_HISTORY_RETRIEVED);
        assertThat(response.getHistory()).hasSize(1);
        assertThat(response.getPagination()).isNotNull();
        verify(monitoringDataDeletionHistoryRepository).findByProjectId(eq(1), any(Pageable.class));
    }

    @Test
    @DisplayName("UT-AMS-04-02: 正常 - 履歴の各項目がレスポンスへ対応する項目に写される")
    void findDeletionHistory_mapsEachField() {
        givenAccessibleProject(1, "PUB-1");
        MonitoringDataDeletionHistory failed = MonitoringDataDeletionHistory.builder()
                .monitoringRecordId("SITEPUBLIC1#PROJPUBLIC1")
                .recordTimeUpdated("2020-01-01T00:00:05Z")
                .dateRecorded("2020-01-02")
                .projectId(1)
                .siteId(10)
                .result(DeletionResult.FAILURE)
                .failureReason("DynamoDB throttled/unprocessed")
                .build();
        when(monitoringDataDeletionHistoryRepository.findByProjectId(eq(1), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(failed)));

        MonitoringDataDeletionHistoryListResponse response = service.findDeletionHistory(1, 1, 20, USER_ID);

        MonitoringDataDeletionHistoryDetailResponse row = response.getHistory().get(0);
        assertThat(row.getMonitoringRecordId()).isEqualTo("SITEPUBLIC1#PROJPUBLIC1");
        assertThat(row.getRecordTimeUpdated()).isEqualTo("2020-01-01T00:00:05Z");
        assertThat(row.getDateRecorded()).isEqualTo("2020-01-02");
        assertThat(row.getProjectId()).isEqualTo(1);
        assertThat(row.getSiteId()).isEqualTo(10);
        assertThat(row.getResult()).isEqualTo(DeletionResult.FAILURE);
        assertThat(row.getFailureReason()).isEqualTo("DynamoDB throttled/unprocessed");
    }

    @Test
    @DisplayName("UT-AMS-04-03: 異常 - 存在しないプロジェクトを指定するとNOT_FOUNDを返し、履歴は読まない")
    void findDeletionHistory_unknownProject_returnsNotFound() {
        when(projectRepository.findById(999)).thenReturn(Optional.empty());

        MonitoringDataDeletionHistoryListResponse response = service.findDeletionHistory(999, 1, 20, USER_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        verifyNoInteractions(monitoringDataDeletionHistoryRepository);
        verifyNoInteractions(projectService);
    }

    @Test
    @DisplayName("UT-AMS-04-04: 異常 - 閲覧権限のないプロジェクトを指定しても、存在しない場合と同じNOT_FOUNDを返す")
    void findDeletionHistory_inaccessibleProject_returnsSameNotFound() {
        // Same answer as an unknown id, so the response does not reveal which ids exist.
        Project project = new Project();
        project.setId(2);
        project.setPublicId("PUB-2");
        when(projectRepository.findById(2)).thenReturn(Optional.of(project));
        when(projectService.hasProjectAccess("PUB-2", USER_ID)).thenReturn(false);

        MonitoringDataDeletionHistoryListResponse response = service.findDeletionHistory(2, 1, 20, USER_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        verifyNoInteractions(monitoringDataDeletionHistoryRepository);
    }

    @Test
    @DisplayName("UT-AMS-04-05: 正常 - projectId未指定の場合、閲覧可能なプロジェクトの履歴だけが対象になる")
    void findDeletionHistory_withoutProjectId_limitedToAccessibleProjects() {
        Project accessible = projectDueOn(1, "PUB-1", LocalDate.of(2030, 1, 1));
        Project hidden = projectDueOn(2, "PUB-2", LocalDate.of(2030, 1, 1));
        givenFinalizedProjects(accessible, hidden);
        when(projectService.hasProjectAccess("PUB-1", USER_ID)).thenReturn(true);
        when(projectService.hasProjectAccess("PUB-2", USER_ID)).thenReturn(false);
        Page<MonitoringDataDeletionHistory> page = new PageImpl<>(List.of(historyOf(1)));
        when(monitoringDataDeletionHistoryRepository.findByProjectIdIn(eq(List.of(1)), any(Pageable.class)))
                .thenReturn(page);

        MonitoringDataDeletionHistoryListResponse response = service.findDeletionHistory(null, 1, 20, USER_ID);

        assertThat(response.getHistory()).hasSize(1);
        verify(monitoringDataDeletionHistoryRepository)
                .findByProjectIdIn(eq(List.of(1)), any(Pageable.class));
        // The unscoped query must never be used again.
        verify(monitoringDataDeletionHistoryRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("UT-AMS-04-06: 正常 - projectId未指定で閲覧可能なプロジェクトが無い場合、OKと空の履歴を返す")
    void findDeletionHistory_withoutProjectIdAndNoAccess_returnsEmpty() {
        givenFinalizedProjects(projectDueOn(2, "PUB-2", LocalDate.of(2030, 1, 1)));
        when(projectService.hasProjectAccess("PUB-2", USER_ID)).thenReturn(false);

        MonitoringDataDeletionHistoryListResponse response = service.findDeletionHistory(null, 1, 20, USER_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getHistory()).isEmpty();
        verifyNoInteractions(monitoringDataDeletionHistoryRepository);
    }

    @Test
    @DisplayName("UT-AMS-04-07: 境界値 - ページ番号は0始まりに変換され、作成日時・idの降順で取得される")
    void findDeletionHistory_buildsZeroBasedPageableSortedNewestFirst() {
        // Distinct page and limit so a swap would fail; page 3 must become index 2.
        givenAccessibleProject(1, "PUB-1");
        Page<MonitoringDataDeletionHistory> empty = new PageImpl<>(List.of());
        when(monitoringDataDeletionHistoryRepository.findByProjectId(eq(1), any(Pageable.class)))
                .thenReturn(empty);

        service.findDeletionHistory(1, 3, 50, USER_ID);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(monitoringDataDeletionHistoryRepository).findByProjectId(eq(1), captor.capture());
        Pageable pageable = captor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(50);
        assertThat(pageable.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(pageable.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    // ─────────────────────────────────────────────────────────────
    // Fixtures
    // ─────────────────────────────────────────────────────────────

    private void givenFinalizedProjects(Project... projects) {
        when(projectRepository.findByProjectStatus(FINALIZED_STATUS)).thenReturn(List.of(projects));
    }

    private void givenLinkedSites(Project project, LinkedSite... sites) {
        when(siteProjectRelationRepository.findLinkedSitesIncludingDeleted(project.getId()))
                .thenReturn(List.of(sites));
    }

    /** A project the caller may see, resolvable by its DB id. */
    private Project givenAccessibleProject(Integer id, String publicId) {
        Project project = new Project();
        project.setId(id);
        project.setPublicId(publicId);
        when(projectRepository.findById(id)).thenReturn(Optional.of(project));
        when(projectService.hasProjectAccess(publicId, USER_ID)).thenReturn(true);
        return project;
    }

    /** A finalized project whose two-year retention ends on the given date. */
    private Project projectDueOn(Integer id, String publicId, LocalDate deletionDate) {
        return projectFinalizedOn(id, publicId, deletionDate.minusYears(2));
    }

    private Project projectFinalizedOn(Integer id, String publicId, LocalDate finalizedOn) {
        Project project = new Project();
        project.setId(id);
        project.setPublicId(publicId);
        project.setProjectName("project-" + id);
        project.setProjectStatus(FINALIZED_STATUS);
        project.setStatusFinalizedAt(finalizedOn.atTime(9, 0));
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
        return recordOf(PK, dateRecorded);
    }

    private EnR002MonitoringRecord recordOf(String id, String dateRecorded) {
        EnR002MonitoringRecord record = new EnR002MonitoringRecord();
        record.setId(id);
        record.setTimeUpdated("2020-01-01T00:00:0" + (recordSequence++) + "Z");
        record.setDateRecorded(dateRecorded);
        return record;
    }

    private Key keyOf(EnR002MonitoringRecord record) {
        return Key.builder().partitionValue(record.getId()).sortValue(record.getTimeUpdated()).build();
    }

    /** DynamoDB hands back the given keys as unprocessed on every call; the rest are deleted. */
    private void givenDeleteLeavesBehind(Key... stuck) {
        Set<Key> stuckKeys = Set.of(stuck);
        when(monitoringTableRecordSolarRepository.deleteBatch(any())).thenAnswer(invocation -> {
            List<Key> requested = invocation.getArgument(0);
            return requested.stream().filter(stuckKeys::contains).toList();
        });
    }

    @SuppressWarnings("unchecked")
    private List<MonitoringDataDeletionHistory> savedHistory() {
        ArgumentCaptor<List<MonitoringDataDeletionHistory>> captor = ArgumentCaptor.forClass(List.class);
        verify(monitoringDataDeletionHistoryRepository, atLeastOnce()).saveAll(captor.capture());
        List<MonitoringDataDeletionHistory> rows = new ArrayList<>();
        captor.getAllValues().forEach(rows::addAll);
        return rows;
    }

    private MonitoringDataDeletionHistory historyOf(Integer projectId) {
        return MonitoringDataDeletionHistory.builder()
                .monitoringRecordId(PK)
                .recordTimeUpdated("2020-01-01T00:00:00Z")
                .dateRecorded("2020-01-01")
                .projectId(projectId)
                .siteId(10)
                .result(DeletionResult.SUCCESS)
                .build();
    }
}
