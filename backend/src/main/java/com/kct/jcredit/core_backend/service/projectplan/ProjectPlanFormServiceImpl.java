package com.kct.jcredit.core_backend.service.projectplan;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.constant.RoleConstants;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanFormBase;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanFormEnvelope;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanFileResponse;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanFormResponse;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanBatchProgressResponse;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanProgressResponse;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanProgressSummary;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanProgressResponse.FormProgress;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.entity.Account;
import com.kct.jcredit.core_backend.entity.OperatorCompany;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.enums.ProjectPlanFormStatus;
import com.kct.jcredit.core_backend.repository.dynamodb.ProjectPlanFormRepository;
import com.kct.jcredit.core_backend.repository.postgres.AccountRepository;
import com.kct.jcredit.core_backend.repository.postgres.OperatingUserRepository;
import com.kct.jcredit.core_backend.service.projectplan.ProjectPlanContextService.ProjectContext;
import com.kct.jcredit.core_backend.service.projectplan.ProjectPlanContextService.ProjectContextStatus;
import com.kct.jcredit.core_backend.util.JsonFieldNameResolver;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectPlanFormServiceImpl implements ProjectPlanFormService {

    private final ProjectPlanContextService projectPlanContextService;
    private final ProjectPlanFormRegistry registry;
    private final ProjectPlanFormRepository repository;
    private final OperatingUserRepository operatingUserRepository;
    private final AccountRepository accountRepository;
    private final ProjectPlanFileService fileService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @Override
    @Transactional(readOnly = true)
    public ProjectPlanFormResponse getForm(
            String projectId,
            String formSlug,
            Integer methodologyIndex,
            String companyId,
            String role,
            String username) {

        ProjectPlanFormResponse response = new ProjectPlanFormResponse();
        response.setFormSlug(formSlug);

        FormDefinition<?> definition = registry.bySlug(formSlug).orElse(null);
        if (definition == null) {
            return formNotDefined(response);
        }
        describe(response, definition);

        ProjectContext context =
                projectPlanContextService.resolveProject(projectId, companyId, role, username);
        if (!context.isAuthorized()) {
            applyContextError(response, context);
            return response;
        }

        return read(definition, projectId, methodologyIndex, context.project(), response);
    }

    @Override
    // readOnly despite the name: the page itself is written to DynamoDB, which
    // is outside this transaction. The only SQL work here is the authorization
    // read. Anything added that writes to PostgreSQL must drop readOnly.
    @Transactional(readOnly = true)
    public ProjectPlanFormResponse saveForm(
            String projectId,
            String formSlug,
            Integer methodologyIndex,
            JsonNode body,
            String companyId,
            String role,
            String username) {

        ProjectPlanFormResponse response = new ProjectPlanFormResponse();
        response.setFormSlug(formSlug);

        FormDefinition<?> definition = registry.bySlug(formSlug).orElse(null);
        if (definition == null) {
            return formNotDefined(response);
        }
        describe(response, definition);

        ProjectContext context =
                projectPlanContextService.resolveProject(projectId, companyId, role, username);
        if (!context.isAuthorized()) {
            applyContextError(response, context);
            return response;
        }

        return write(definition, projectId, methodologyIndex, body, username, response);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectPlanProgressResponse getProgress(
            String projectId, String companyId, String role, String username) {

        ProjectPlanProgressResponse response = new ProjectPlanProgressResponse();

        ProjectContext context =
                projectPlanContextService.resolveProject(projectId, companyId, role, username);
        if (!context.isAuthorized()) {
            applyContextError(response, context);
            return response;
        }

        List<FormProgress> forms = pagesOf(projectId);

        response.setForms(forms);
        response.setTotalForms(forms.size());
        response.setCompletedForms(completedCount(forms));
        response.setNextIncompleteSlug(nextIncompleteSlug(forms));
        response.setStatusCode(ResponseConstants.OK);
        response.setMessage(ResponseConstants.MSG_PROGRESS_RETRIEVED);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectPlanBatchProgressResponse getProgressForProjects(
            List<String> projectIds, String companyId, String role, String username) {

        ProjectPlanBatchProgressResponse response = new ProjectPlanBatchProgressResponse();

        List<ProjectPlanProgressSummary> summaries = (projectIds == null ? List.<String>of() : projectIds)
                .stream()
                .distinct()
                .filter(id -> id != null && !id.isBlank())
                // Skip rather than fail: one project the caller cannot see must
                // not blank out progress for the whole list.
                .filter(id -> projectPlanContextService
                        .resolveProject(id, companyId, role, username)
                        .isAuthorized())
                .map(id -> {
                    List<FormProgress> forms = pagesOf(id);
                    String next = nextIncompleteSlug(forms);
                    return new ProjectPlanProgressSummary(
                            id, forms.size(), completedCount(forms), next, next == null);
                })
                .toList();

        response.setProjects(summaries);
        response.setStatusCode(ResponseConstants.OK);
        response.setMessage(ResponseConstants.MSG_PROGRESS_RETRIEVED);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    // --- progress helpers, shared by the single and batch endpoints ---

    /** Every registered page with its stored status for one project. */
    private List<FormProgress> pagesOf(String projectId) {
        Map<String, ProjectPlanFormEnvelope> storedBySlug =
                repository.findEnvelopes(buildPk(projectId)).stream()
                        .filter(envelope -> registry.bySortKey(envelope.getSk()).isPresent())
                        .collect(Collectors.toMap(
                                envelope -> registry.bySortKey(envelope.getSk()).orElseThrow().slug(),
                                Function.identity(),
                                (first, second) -> first));

        return registry.all().stream()
                .map(definition -> {
                    ProjectPlanFormEnvelope stored = storedBySlug.get(definition.slug());
                    return new FormProgress(
                            definition.slug(),
                            definition.displayName(),
                            definition.order(),
                            statusOf(stored),
                            stored == null ? null : stored.getUpdatedAt(),
                            stored == null ? null : stored.getUpdatedBy());
                })
                .toList();
    }

    private static int completedCount(List<FormProgress> forms) {
        return (int) forms.stream()
                .filter(form -> ProjectPlanFormStatus.COMPLETE.name().equals(form.getStatus()))
                .count();
    }

    private static String nextIncompleteSlug(List<FormProgress> forms) {
        return forms.stream()
                .filter(form -> !ProjectPlanFormStatus.COMPLETE.name().equals(form.getStatus()))
                .map(FormProgress::getFormSlug)
                .findFirst()
                .orElse(null);
    }

    // --- typed helpers: capture the registry's wildcard once ---

    private <T extends ProjectPlanFormBase> ProjectPlanFormResponse read(
            FormDefinition<T> definition,
            String projectId,
            Integer methodologyIndex,
            Project project,
            ProjectPlanFormResponse response) {

        String pk = buildPk(projectId);
        String sk = definition.sk(methodologyIndex);

        T stored = repository.find(definition.type(), pk, sk).orElse(null);
        boolean exists = stored != null;
        T item = exists ? stored : definition.newInstance();

        // PostgreSQL stays authoritative for the fields it can supply, on every
        // read. Only those are reported read-only: locking a field SQL left
        // empty would make a required page impossible to complete.
        response.setReadOnlyFields(definition.applySql(item, sqlContextFor(project), objectMapper));
        item.setPk(pk);
        item.setSk(sk);

        response.setData(item);
        response.setStatus(exists
                ? ProjectPlanFormStatus.COMPLETE.name()
                : ProjectPlanFormStatus.NOT_STARTED.name());
        response.setUpdatedAt(item.getUpdatedAt());
        response.setUpdatedBy(item.getUpdatedBy());
        response.setStatusCode(ResponseConstants.OK);
        response.setMessage(ResponseConstants.MSG_FORM_RETRIEVED);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    private <T extends ProjectPlanFormBase> ProjectPlanFormResponse write(
            FormDefinition<T> definition,
            String projectId,
            Integer methodologyIndex,
            JsonNode body,
            String username,
            ProjectPlanFormResponse response) {

        T payload;
        try {
            payload = objectMapper.treeToValue(body, definition.type());
        } catch (JsonProcessingException e) {
            log.warn("Invalid project-plan form body for slug={}", definition.slug(), e);
            response.setStatusCode(ResponseConstants.BAD_REQUEST);
            response.setMessage(ResponseConstants.MSG_BAD_REQUEST);
            response.setTimestamp(LocalDateTime.now());
            return response;
        }
        if (payload == null) {
            payload = definition.newInstance();
        }

        // A page is all-or-nothing: nothing is persisted unless it fully validates.
        Map<String, String> errors = validate(payload);
        if (!errors.isEmpty()) {
            response.setStatusCode(ResponseConstants.VALIDATION_ERROR);
            response.setMessage(ResponseConstants.MSG_VALIDATION_ERROR);
            response.setErrors(errors);
            response.setTimestamp(LocalDateTime.now());
            return response;
        }

        // The page is good, so its uploads become permanent and the payload is
        // re-read so the stored keys are the permanent ones. Doing this after
        // validation is what keeps a rejected page from leaving files behind:
        // its uploads stay under tmp/ and expire.
        JsonNode promoted = fileService.promoteUploads(body, projectId, definition.slug());
        if (promoted != body) {
            try {
                payload = objectMapper.treeToValue(promoted, definition.type());
            } catch (JsonProcessingException e) {
                // Only text values changed, so this cannot realistically fail —
                // but the files have already moved, so say so rather than
                // reporting a plain bad request.
                log.error("Promoted uploads but could not re-read page slug={}", definition.slug(), e);
                throw new IllegalStateException("Failed to re-read project-plan page after upload promotion", e);
            }
        }

        // Keys and audit metadata are server-managed and never taken from clients.
        payload.setPk(buildPk(projectId));
        payload.setSk(definition.sk(methodologyIndex));
        payload.setStatus(ProjectPlanFormStatus.COMPLETE.name());
        payload.setUpdatedAt(Instant.now());
        payload.setUpdatedBy(username);

        repository.save(definition.type(), payload);

        response.setData(payload);
        response.setStatus(payload.getStatus());
        response.setUpdatedAt(payload.getUpdatedAt());
        response.setUpdatedBy(payload.getUpdatedBy());
        response.setStatusCode(ResponseConstants.OK);
        response.setMessage(ResponseConstants.MSG_FORM_SAVED);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    /**
     * Mirrors {@code GlobalErrorHandler}'s {@code @Valid} path so programmatic
     * validation produces an identical {@code errors} contract.
     */
    private Map<String, String> validate(Object target) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ConstraintViolation<Object> violation : validator.validate(target)) {
            String field = JsonFieldNameResolver.resolve(
                    target.getClass(), violation.getPropertyPath().toString());
            errors.putIfAbsent(field, violation.getMessage());
        }
        return errors;
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectPlanFileResponse uploadFormFile(
            String projectId,
            String formSlug,
            String fieldName,
            MultipartFile file,
            String companyId,
            String role,
            String username) {

        ProjectPlanFileResponse response = new ProjectPlanFileResponse();

        // An unknown page is refused before anything reaches S3, so a typo
        // cannot litter the bucket with files no page will ever claim.
        FormDefinition<?> definition = registry.bySlug(formSlug).orElse(null);
        if (definition == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_FORM_NOT_DEFINED);
            response.setTimestamp(LocalDateTime.now());
            return response;
        }

        ProjectContext context =
                projectPlanContextService.resolveProject(projectId, companyId, role, username);
        if (!context.isAuthorized()) {
            applyContextError(response, context);
            return response;
        }

        var uploaded = fileService.uploadToTemp(projectId, formSlug, fieldName, username, file);
        response.setS3Key(uploaded.getS3Key());
        response.setFileId(uploaded.getFileId());
        response.setOriginalName(uploaded.getOriginalName());
        response.setStatusCode(ResponseConstants.OK);
        response.setMessage(ResponseConstants.MSG_FORM_SAVED);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    private ProjectPlanSqlContext sqlContextFor(Project project) {
        return new ProjectPlanSqlContext(
                project,
                () -> {
                    OperatorCompany company = companyOf(project);
                    if (company == null) {
                        return null;
                    }
                    return operatingUserRepository
                            .findByCompany_IdAndRole(company.getId(), RoleConstants.MASTER)
                            .orElse(null);
                },
                () -> {
                    OperatorCompany company = companyOf(project);
                    if (company == null) {
                        return null;
                    }
                    List<Account> accounts =
                            accountRepository.findByOperatorCompany_IdAndIsActiveTrue(company.getId());
                    // Only autofill when there is no doubt which account it is.
                    // With several active accounts the holder is a business
                    // question, and a wrong guess would be written over the
                    // operator's own answer on every read.
                    if (accounts.size() != 1) {
                        if (accounts.size() > 1) {
                            log.warn("Company {} has {} active accounts; leaving 保有者 fields to the operator",
                                    company.getCompanyId(), accounts.size());
                        }
                        return null;
                    }
                    return accounts.get(0);
                });
    }

    private static OperatorCompany companyOf(Project project) {
        return project == null ? null : project.getOperatorCompany();
    }

    private static String statusOf(ProjectPlanFormEnvelope envelope) {
        if (envelope == null) {
            return ProjectPlanFormStatus.NOT_STARTED.name();
        }
        // Items written before `status` existed read back null; they were only
        // ever persisted after passing full validation, so they are complete.
        return Optional.ofNullable(envelope.getStatus())
                .orElse(ProjectPlanFormStatus.COMPLETE.name());
    }

    /**
     * Page metadata that holds regardless of the project. {@code readOnlyFields}
     * is deliberately not here: which fields are read-only depends on what
     * PostgreSQL actually had for this project, so {@link #read} sets it.
     */
    private static void describe(ProjectPlanFormResponse response, FormDefinition<?> definition) {
        response.setDisplayName(definition.displayName());
        response.setConditionalFields(definition.conditionalFields());
    }

    private static ProjectPlanFormResponse formNotDefined(ProjectPlanFormResponse response) {
        response.setStatusCode(ResponseConstants.NOT_FOUND);
        response.setMessage(ResponseConstants.MSG_FORM_NOT_DEFINED);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    private static void applyContextError(BaseResponse response, ProjectContext context) {
        if (context.status() == ProjectContextStatus.NOT_FOUND) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_PROJECT_NOT_FOUND);
        } else {
            response.setStatusCode(ResponseConstants.FORBIDDEN);
            response.setMessage(ResponseConstants.MSG_FORBIDDEN);
        }
        response.setTimestamp(LocalDateTime.now());
    }

    private static String buildPk(String projectId) {
        return String.valueOf(projectId);
    }
}
