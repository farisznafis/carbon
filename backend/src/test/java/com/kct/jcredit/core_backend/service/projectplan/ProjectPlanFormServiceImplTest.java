package com.kct.jcredit.core_backend.service.projectplan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.projectplan.ApplicableConditionsRequest;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectOverviewRequest;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanFormEnvelope;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanFormResponse;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanProgressResponse;
import com.kct.jcredit.core_backend.entity.Account;
import com.kct.jcredit.core_backend.entity.OperatorCompany;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.enums.ProjectPlanFormStatus;
import com.kct.jcredit.core_backend.repository.dynamodb.ProjectPlanFormRepository;
import com.kct.jcredit.core_backend.repository.postgres.AccountRepository;
import com.kct.jcredit.core_backend.repository.postgres.OperatingUserRepository;
import com.kct.jcredit.core_backend.service.projectplan.ProjectPlanContextService.ProjectContext;
import com.kct.jcredit.core_backend.service.projectplan.ProjectPlanContextService.ProjectContextStatus;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Behaviour of the registry-driven form service.
 *
 * <p>The load-bearing guarantee is page atomicity: an invalid page must leave
 * DynamoDB untouched, because that is what lets the progress endpoint be
 * trusted to say where an operator should resume.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProjectPlanFormServiceImplTest {

    private static final String PROJECT_ID = "PROJ1";
    private static final String SLUG = "applicable-conditions";
    private static final String SK = "APPLICABLE_CONDITIONS";

    @Mock private ProjectPlanContextService contextService;
    @Mock private ProjectPlanFormRepository repository;
    @Mock private OperatingUserRepository operatingUserRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private ProjectPlanFileService fileService;
    @Mock private Project project;
    @Mock private OperatorCompany company;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    private ProjectPlanFormServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProjectPlanFormServiceImpl(
                contextService,
                new ProjectPlanFormRegistry(),
                repository,
                operatingUserRepository,
                accountRepository,
                fileService,
                objectMapper,
                validator);

        when(company.getId()).thenReturn(1);
        when(company.getNameJa()).thenReturn("株式会社テスト");
        when(company.getRepPosition()).thenReturn("代表取締役");
        when(company.getRepLastName()).thenReturn("山田");
        when(company.getRepFirstName()).thenReturn("太郎");

        when(project.getOperatorCompany()).thenReturn(company);
        when(project.getMethodCategory()).thenReturn("EN-R");
        when(project.getMethodNumber()).thenReturn("002");
        when(project.getMethodologyVersion()).thenReturn("Ver.2.0");

        authorize();
        // Pages without uploads pass through untouched; the promotion path has
        // its own tests.
        when(fileService.promoteUploads(any(), anyString(), anyString()))
                .thenAnswer(call -> call.getArgument(0));
    }

    private void authorize() {
        when(contextService.resolveProject(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new ProjectContext(ProjectContextStatus.AUTHORIZED, project));
    }

    private ProjectPlanFormResponse save(JsonNode body) {
        return service.saveForm(PROJECT_ID, SLUG, null, body, "C1", "operator", "u1");
    }

    private static JsonNode validPage(ObjectMapper mapper) {
        return mapper.createObjectNode()
                .put("methodologyVersion", "Ver.2.0")
                .put("esgPledgeCase", "適用条件１")
                .put("esgPledgeCaseDate", "2026-08-12")
                .put("businessName", "株式会社テスト")
                .put("representativePosition", "代表取締役");
    }

    // --- page atomicity ---

    @Test
    void anIncompletePageIsRejectedAndNothingIsStored() {
        // esgPledgeCase omitted -> the page is not valid as a whole.
        JsonNode body = validPage(objectMapper).deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) body).remove("esgPledgeCase");

        ProjectPlanFormResponse response = save(body);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.VALIDATION_ERROR);
        assertThat(response.getErrors()).containsKey("esgPledgeCase");
        verify(repository, never()).save(any(), any());
    }

    @Test
    void aCompletePageIsStoredAsCompleteWithServerManagedKeys() {
        ProjectPlanFormResponse response = save(validPage(objectMapper));

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);

        ArgumentCaptor<ApplicableConditionsRequest> saved =
                ArgumentCaptor.forClass(ApplicableConditionsRequest.class);
        verify(repository).save(eq(ApplicableConditionsRequest.class), saved.capture());

        ApplicableConditionsRequest item = saved.getValue();
        assertThat(item.getPk()).isEqualTo(PROJECT_ID);
        assertThat(item.getSk()).isEqualTo(SK);
        assertThat(item.getStatus()).isEqualTo(ProjectPlanFormStatus.COMPLETE.name());
        assertThat(item.getUpdatedBy()).isEqualTo("u1");
        assertThat(item.getUpdatedAt()).isNotNull();
    }

    @Test
    void clientsCannotForgeCompletionMetadata() {
        com.fasterxml.jackson.databind.node.ObjectNode body =
                (com.fasterxml.jackson.databind.node.ObjectNode) validPage(objectMapper);
        body.put("updatedBy", "someone-else");

        save(body);

        ArgumentCaptor<ApplicableConditionsRequest> saved =
                ArgumentCaptor.forClass(ApplicableConditionsRequest.class);
        verify(repository).save(eq(ApplicableConditionsRequest.class), saved.capture());
        assertThat(saved.getValue().getUpdatedBy()).isEqualTo("u1");
    }

    // --- guards ---

    @Test
    void anUnknownSlugIsRejectedBeforeAnythingElseHappens() {
        ProjectPlanFormResponse response =
                service.getForm(PROJECT_ID, "no-such-page", null, "C1", "operator", "u1");

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        verify(contextService, never()).resolveProject(any(), any(), any(), any());
    }

    @Test
    void anUnauthorizedCallerNeverReachesStorage() {
        when(contextService.resolveProject(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new ProjectContext(ProjectContextStatus.FORBIDDEN, null));

        ProjectPlanFormResponse response = save(validPage(objectMapper));

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
        verify(repository, never()).save(any(), any());
    }

    // --- reads ---

    @Test
    void anUnsavedPageReadsAsNotStartedButStillCarriesItsMetadata() {
        when(repository.find(eq(ApplicableConditionsRequest.class), anyString(), anyString()))
                .thenReturn(Optional.empty());

        ProjectPlanFormResponse response =
                service.getForm(PROJECT_ID, SLUG, null, "C1", "operator", "u1");

        assertThat(response.getStatus()).isEqualTo(ProjectPlanFormStatus.NOT_STARTED.name());
        assertThat(response.getReadOnlyFields()).contains("businessName", "methodologyCode");
        assertThat(response.getData()).isNotNull();
    }

    @Test
    void sqlBackedFieldsAreOverlaidOnRead() {
        ApplicableConditionsRequest stored = new ApplicableConditionsRequest();
        stored.setBusinessName("古い会社名");
        when(repository.find(eq(ApplicableConditionsRequest.class), anyString(), anyString()))
                .thenReturn(Optional.of(stored));

        ProjectPlanFormResponse response =
                service.getForm(PROJECT_ID, SLUG, null, "C1", "operator", "u1");

        ApplicableConditionsRequest data = (ApplicableConditionsRequest) response.getData();
        // PostgreSQL is authoritative for these, so the stale stored value loses.
        assertThat(data.getBusinessName()).isEqualTo("株式会社テスト");
        assertThat(data.getMethodologyCode()).isEqualTo("EN-R-002");
        assertThat(data.getRepresentativeName()).isEqualTo("山田 太郎");
    }

    // --- progress / resume ---

    @Test
    void progressReportsCompletedPagesAndWhereToResume() {
        ProjectPlanFormEnvelope done = new ProjectPlanFormEnvelope();
        done.setSk("PROJECT_APPLICATION");
        done.setStatus(ProjectPlanFormStatus.COMPLETE.name());
        when(repository.findEnvelopes(PROJECT_ID)).thenReturn(List.of(done));

        ProjectPlanProgressResponse response =
                service.getProgress(PROJECT_ID, "C1", "operator", "u1");

        assertThat(response.getCompletedForms()).isEqualTo(1);
        // Derived, not hardcoded, so registering another page does not break
        // this. Optional pages are not part of the total.
        assertThat(response.getTotalForms()).isEqualTo(
                (int) new ProjectPlanFormRegistry().all().stream()
                        .filter(definition -> !definition.optional())
                        .count());
        // The application page is done, so the operator resumes at the next one.
        assertThat(response.getNextIncompleteSlug()).isEqualTo("other-operator-projects");
    }

    @Test
    void anOptionalPageIsListedButNeitherCountedNorResumedInto() {
        // 認証権限保有主体変更届 is filed only when the project's owner changes.
        // Counted, a plan that never files it could never read complete; offered
        // as 続きから, every operator would be parked on a form they must not
        // fill in.
        when(repository.findEnvelopes(PROJECT_ID)).thenReturn(List.of());

        ProjectPlanProgressResponse response =
                service.getProgress(PROJECT_ID, "C1", "operator", "u1");

        assertThat(response.getForms())
                .filteredOn(form -> "authority-holder-change".equals(form.getFormSlug()))
                .singleElement()
                .satisfies(form -> assertThat(form.isOptional()).isTrue());

        assertThat(response.getTotalForms())
                .isEqualTo(new ProjectPlanFormRegistry().all().size() - 1);
        assertThat(response.getNextIncompleteSlug()).isEqualTo("application");
    }

    @Test
    void aPlanIsCompleteEvenWithTheOptionalPageUntouched() {
        List<ProjectPlanFormEnvelope> everyRequiredPage =
                new ProjectPlanFormRegistry().all().stream()
                        .filter(definition -> !definition.optional())
                        .map(definition -> {
                            ProjectPlanFormEnvelope envelope = new ProjectPlanFormEnvelope();
                            envelope.setSk(definition.skPrefix());
                            envelope.setStatus(ProjectPlanFormStatus.COMPLETE.name());
                            return envelope;
                        })
                        .toList();
        when(repository.findEnvelopes(PROJECT_ID)).thenReturn(everyRequiredPage);

        ProjectPlanProgressResponse response =
                service.getProgress(PROJECT_ID, "C1", "operator", "u1");

        assertThat(response.getNextIncompleteSlug()).isNull();
        assertThat(response.getCompletedForms()).isEqualTo(response.getTotalForms());
    }

    @Test
    void pagesStoredBeforeStatusExistedAreTreatedAsComplete() {
        ProjectPlanFormEnvelope legacy = new ProjectPlanFormEnvelope();
        legacy.setSk("PROJECT_APPLICATION");
        legacy.setStatus(null);
        when(repository.findEnvelopes(PROJECT_ID)).thenReturn(List.of(legacy));

        ProjectPlanProgressResponse response =
                service.getProgress(PROJECT_ID, "C1", "operator", "u1");

        // They were only ever written after passing full validation.
        assertThat(response.getCompletedForms()).isEqualTo(1);
    }
    // --- autofill claims only the fields PostgreSQL can actually fill ---
    //
    // readOnlyFields drives the frontend to disable an input. Reporting a field
    // SQL left empty would leave it blank *and* locked, and holder*/company
    // fields are @NotBlank — such a page could never be saved by anyone.

    private static final String OVERVIEW_SLUG = "project-overview";

    private ProjectPlanFormResponse readOverview() {
        return service.getForm(PROJECT_ID, OVERVIEW_SLUG, null, "C1", "operator", "u1");
    }

    private static Account account(String holderName) {
        Account account = new Account();
        account.setHolderAccountName(holderName);
        account.setHolderAccountNameKana("カブシキガイシャテスト");
        account.setHolderPrefecture("東京都");
        account.setHolderCity("千代田区");
        account.setHolderAddress("1-1-1");
        account.setIsHolderUndecided(false);
        return account;
    }

    private void storedOverview(ProjectOverviewRequest stored) {
        when(repository.find(eq(ProjectOverviewRequest.class), anyString(), anyString()))
                .thenReturn(Optional.ofNullable(stored));
    }

    @Test
    void holderFieldsComeFromTheCompanysSingleActiveAccount() {
        when(accountRepository.findByOperatorCompany_IdAndIsActiveTrue(1))
                .thenReturn(List.of(account("Ｊクレ保有者株式会社")));
        storedOverview(null);

        ProjectPlanFormResponse response = readOverview();

        ProjectOverviewRequest data = (ProjectOverviewRequest) response.getData();
        assertThat(data.getHolderAccountName()).isEqualTo("Ｊクレ保有者株式会社");
        assertThat(data.getHolderCity()).isEqualTo("千代田区");
        assertThat(response.getReadOnlyFields())
                .contains("holderAccountName", "holderCity", "isHolderUndecided");
    }

    @Test
    void severalActiveAccountsLeaveTheHolderToTheOperator() {
        when(accountRepository.findByOperatorCompany_IdAndIsActiveTrue(1))
                .thenReturn(List.of(account("Ａ社"), account("Ｂ社")));
        ProjectOverviewRequest stored = new ProjectOverviewRequest();
        stored.setHolderAccountName("運用者が入力した保有者");
        storedOverview(stored);

        ProjectPlanFormResponse response = readOverview();

        // Which account is the holder is a business question, so neither one is
        // guessed at — and the operator's own answer survives the read.
        ProjectOverviewRequest data = (ProjectOverviewRequest) response.getData();
        assertThat(data.getHolderAccountName()).isEqualTo("運用者が入力した保有者");
        assertThat(response.getReadOnlyFields()).doesNotContain("holderAccountName");
    }

    @Test
    void noAccountLeavesTheHolderFieldsOpenRatherThanLockedAndEmpty() {
        when(accountRepository.findByOperatorCompany_IdAndIsActiveTrue(1)).thenReturn(List.of());
        storedOverview(null);

        ProjectPlanFormResponse response = readOverview();

        assertThat(response.getReadOnlyFields())
                .doesNotContain("holderAccountName", "holderCity", "isHolderUndecided");
    }

    @Test
    void aFieldWithNoSqlValueIsNeitherOverwrittenNorLocked() {
        when(company.getNameFurigana()).thenReturn(null);
        when(accountRepository.findByOperatorCompany_IdAndIsActiveTrue(1)).thenReturn(List.of());
        ProjectOverviewRequest stored = new ProjectOverviewRequest();
        stored.setProjectOperatorKana("カブシキガイシャテスト");
        storedOverview(stored);

        ProjectPlanFormResponse response = readOverview();

        ProjectOverviewRequest data = (ProjectOverviewRequest) response.getData();
        assertThat(data.getProjectOperatorKana()).isEqualTo("カブシキガイシャテスト");
        assertThat(response.getReadOnlyFields()).doesNotContain("projectOperatorKana");
        // The company name is present, so that one stays SQL-authoritative.
        assertThat(response.getReadOnlyFields()).contains("projectOperatorName");
    }
    // --- uploads follow the page, not the request ---

    @Test
    void aRejectedPageLeavesItsUploadsInTheProvisionalArea() {
        JsonNode body = validPage(objectMapper).deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) body).remove("esgPledgeCase");

        save(body);

        // Same guarantee as page atomicity: a page that does not validate must
        // move nothing, so the files expire under tmp/ instead of being
        // stranded in permanent storage with no record pointing at them.
        verify(fileService, never()).promoteUploads(any(), anyString(), anyString());
        verify(repository, never()).save(any(), any());
    }

    @Test
    void aStoredPageHasItsUploadsPromotedFirst() {
        save(validPage(objectMapper));

        var order = org.mockito.Mockito.inOrder(fileService, repository);
        order.verify(fileService).promoteUploads(any(), eq(PROJECT_ID), eq(SLUG));
        order.verify(repository).save(any(), any());
    }
}
