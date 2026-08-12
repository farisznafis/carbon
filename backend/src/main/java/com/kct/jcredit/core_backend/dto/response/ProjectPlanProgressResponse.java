package com.kct.jcredit.core_backend.dto.response;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * How far through the plan a project is, so the operator can stop and resume.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectPlanProgressResponse extends BaseResponse {

    private int totalForms;
    private int completedForms;

    /** First page that is not yet complete, or {@code null} when the plan is done. */
    private String nextIncompleteSlug;

    private List<FormProgress> forms;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FormProgress {
        private String formSlug;
        private String displayName;
        private int order;
        private String status;
        private Instant updatedAt;
        private String updatedBy;
    }
}
