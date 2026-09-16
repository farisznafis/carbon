package com.kct.jcredit.core_backend.enums;

/**
 * How ready a row is to be copied into the official 別紙10-1 spreadsheet.
 */
public enum ReportReadiness {
    /** Equipment evidence files and batch results are both present. */
    READY,
    /** Batch results exist but required equipment evidence files are missing. */
    MISSING_EVIDENCE,
    /** No batch result yet — monitoring data has never been processed for this equipment. */
    NO_BATCH_RESULT
}