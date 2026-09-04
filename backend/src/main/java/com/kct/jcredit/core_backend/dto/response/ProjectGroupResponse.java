package com.kct.jcredit.core_backend.dto.response;

import com.kct.jcredit.core_backend.enums.PeriodStatus;


import java.time.LocalDate;
import java.util.List;


public record ProjectGroupResponse(
        String projectId,
        String projectName,
        List<PeriodGroup> periods
) {
 
    public record PeriodGroup(
            String periodId,
            PeriodStatus periodStatus,
            LocalDate startDate,
            LocalDate endDate,
            List<RowSummaryActivityResponse> rows
    ) {}
}
