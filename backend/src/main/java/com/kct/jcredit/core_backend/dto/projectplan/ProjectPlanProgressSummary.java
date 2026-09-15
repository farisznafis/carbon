package com.kct.jcredit.core_backend.dto.projectplan;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * How far one project's plan is filled, without the per-page detail.
 *
 * <p>Enough for a project list to show progress and decide whether the action
 * is "resume where you stopped" or "pick a page to update".
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectPlanProgressSummary {

    private String projectId;
    private int totalForms;
    private int completedForms;

    /** First page not yet complete, or {@code null} once every page is done. */
    private String nextIncompleteSlug;

    /** True when nothing is left to fill, so the caller can offer a free choice. */
    private boolean complete;
}
