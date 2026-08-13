package com.kct.jcredit.core_backend.service.projectplan;

import java.util.function.BiConsumer;

/**
 * One field whose value comes from PostgreSQL rather than from the operator.
 *
 * <p>A single declaration serves both purposes: {@link #apply} performs the
 * overlay when a page is read, and {@link #jsonName} is reported to the client
 * as read-only. Keeping them together is what stops the overlay and the
 * read-only list drifting apart as the form grows past 30 pages.
 *
 * @param jsonName the JSON property name, as the frontend sees it
 * @param apply    copies the value from the SQL context onto the form
 */
public record SqlField<T>(String jsonName, BiConsumer<T, ProjectPlanSqlContext> apply) {
}
