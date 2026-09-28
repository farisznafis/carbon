package com.kct.jcredit.core_backend.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * Rows belonging to one company, grouped by the project they participate in.
 * Projects the current user may not access are omitted entirely, not masked.
 */
public record CompanyActivityGroupResponse(
        String projectId,
        String projectName,
        List<EnR002IndividualActivityRowResponse> rows
) {}




