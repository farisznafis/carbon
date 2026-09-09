package com.kct.jcredit.core_backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
 
import com.kct.jcredit.core_backend.enums.PeriodStatus;
 
public record PeriodSummaryResponse(
        String periodId,
        PeriodStatus periodStatus,
        LocalDate periodStartDate,
        LocalDate periodEndDate,
        LocalDateTime generatedAt,
        BigDecimal totalReductionTco2,
        long rowCount
) {}
 