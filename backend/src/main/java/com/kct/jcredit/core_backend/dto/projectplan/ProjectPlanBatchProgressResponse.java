package com.kct.jcredit.core_backend.dto.projectplan;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Plan progress for several projects at once, so a list can show it in one
 * round trip instead of one request per row.
 *
 * <p>Projects the caller may not see are omitted rather than reported as an
 * error: one inaccessible project should not blank out the whole list.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectPlanBatchProgressResponse extends BaseResponse {

    private List<ProjectPlanProgressSummary> projects;
}
