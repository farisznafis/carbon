package com.kct.jcredit.core_backend.dto.response;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.service.projectplan.FormDefinition.ConditionalField;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * One page of the project plan, plus the metadata the client needs to render it.
 *
 * <p>{@code data} is typed {@code Object} because the concrete DTO is resolved
 * from the registry at runtime; Jackson serialises by runtime class, so the
 * payload is unaffected.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectPlanFormResponse extends BaseResponse {

    private String formSlug;
    private String displayName;

    /** {@code NOT_STARTED} or {@code COMPLETE}. */
    private String status;

    private Instant updatedAt;
    private String updatedBy;

    /** Autofilled from PostgreSQL — the client must render these read-only. */
    private List<String> readOnlyFields;

    /** 有/無 rules driving which fields the client shows and requires. */
    private List<ConditionalField> conditionalFields;

    private Object data;
}
