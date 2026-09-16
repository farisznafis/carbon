package com.kct.jcredit.core_backend.dto.response;

import java.math.BigDecimal;

import com.kct.jcredit.core_backend.enums.EligibilityStatus;
import com.kct.jcredit.core_backend.enums.ReportReadiness;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
 

/**
 * One row of the individual activity list — corresponds to exactly one row
 * of the official 別紙10-1 spreadsheet, i.e. one equipment.
 */

public record EnR002IndividualActivityRowResponse(
        Integer equipmentId,
        String companyId,
        String companyName,
        String siteId,
        String siteAddress,
        boolean hasBattery,
        EligibilityStatus eligibilityStatus,
        ReportReadiness reportReadiness,
        /** Cumulative generated energy at the end of the current reporting window (発電量積算値). */
        BigDecimal generationKwh,
        /** Cumulative sold energy at the end of the current reporting window (売電量積算値). */
        BigDecimal saleKwh
) {}
 
 
