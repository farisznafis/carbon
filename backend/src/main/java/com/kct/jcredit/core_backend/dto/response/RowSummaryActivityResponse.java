package com.kct.jcredit.core_backend.dto.response;

import java.math.BigDecimal;
 
import com.kct.jcredit.core_backend.enums.CompletenessStatus;
import com.kct.jcredit.core_backend.enums.EligibilityStatus;
import com.kct.jcredit.core_backend.enums.PeriodStatus;
 
public record RowSummaryActivityResponse(
        String rowKey,
        PeriodStatus rowType,
        String companyId,
        String companyName,
        String siteId,
        String siteAddress,
        boolean hasBattery,
        EligibilityStatus eligibilityStatus,
        CompletenessStatus completenessStatus,
        BigDecimal reductionResultTco2
) {}
 
