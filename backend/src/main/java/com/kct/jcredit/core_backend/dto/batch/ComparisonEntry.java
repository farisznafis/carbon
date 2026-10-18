package com.kct.jcredit.core_backend.dto.batch;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ComparisonEntry {
    private Integer siteId;
    private String targetStatus;
    private String resultStatus;
    private String matchStatus; // MATCH, MISMATCH, DUPLICATE, MISSING
    private String issueType;   // INFO, WARNING, ERROR, (blank)
    private String details;
}