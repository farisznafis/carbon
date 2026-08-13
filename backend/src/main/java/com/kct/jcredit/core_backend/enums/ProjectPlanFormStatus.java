package com.kct.jcredit.core_backend.enums;

/**
 * Completion state of a single project-plan page.
 *
 * <p>A page is saved only when it is fully valid, so there is no partial state:
 * either the page has never been saved ({@link #NOT_STARTED}) or it is complete.
 * {@code NOT_STARTED} is never persisted — it is inferred when no DynamoDB item
 * exists for the page.
 */
public enum ProjectPlanFormStatus {
    NOT_STARTED,
    COMPLETE
}
