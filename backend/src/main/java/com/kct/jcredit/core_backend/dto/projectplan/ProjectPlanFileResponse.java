package com.kct.jcredit.core_backend.dto.projectplan;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * A provisional upload against a project-plan page.
 *
 * <p>{@code s3Key} still points under {@code tmp/}. The client puts it into the
 * page body; it is promoted to a permanent key only when that page is saved and
 * validates.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectPlanFileResponse extends BaseResponse {

    private String s3Key;
    private String fileId;
    private String originalName;
}
