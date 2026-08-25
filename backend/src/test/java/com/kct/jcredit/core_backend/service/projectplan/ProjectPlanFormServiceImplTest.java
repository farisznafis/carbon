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
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanFormEnvelope;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanFormResponse;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanProgressResponse;
import com.kct.jcredit.core_backend.entity.OperatorCompany;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.enums.ProjectPlanFormStatus;
import com.kct.jcredit.core_backend.repository.dynamodb.ProjectPlanFormRepository;
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
        // Derived, not hardcoded, so registering another page does not break this.
        assertThat(response.getTotalForms())
                .isEqualTo(new ProjectPlanFormRegistry().all().size());
        // The application page is done, so the operator resumes at the next one.
        assertThat(response.getNextIncompleteSlug()).isEqualTo("other-operator-projects");
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
}
