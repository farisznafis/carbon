package com.kct.jcredit.core_backend.dto.projectplan;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * A short-lived URL to view one file attached to a saved project-plan page.
 *
 * <p>Only permanent keys of the requested project are signed; a provisional
 * {@code tmp/} key never is, because nothing has saved it yet.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectPlanFileDownloadResponse extends BaseResponse {

    private String url;
    private Long expiresIn;
}
