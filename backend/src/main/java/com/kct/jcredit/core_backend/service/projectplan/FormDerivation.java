package com.kct.jcredit.core_backend.service.projectplan;

import java.util.List;

import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanFormBase;

/**
 * Values a page computes rather than asks for.
 *
 * <p>Distinct from {@link SqlField}, which copies a value PostgreSQL already
 * holds. A derivation produces something that exists nowhere until it is
 * calculated — the 年度 skeleton of 4.排出削減計画, the 合計 row under its table,
 * the 年度内日数 of each fiscal year. The official sheets compute all of these
 * with formulas, and a form that asks an operator to type them is asking them
 * to reproduce arithmetic the sheet does for free, then getting it wrong in a
 * way nothing checks.
 *
 * <p>Runs on <b>every read and every save</b>, and overwrites whatever the
 * client sent. That is what makes it safe to send derived values to the client
 * at all: a total the server recomputes cannot be forged by posting a different
 * one. {@link #derivedFields()} names them so they arrive read-only.
 *
 * <p>Must be idempotent — save applies it twice, once before validation and
 * once after any file uploads are promoted.
 */
public interface FormDerivation<T extends ProjectPlanFormBase> {

    /**
     * The JSON names this derivation owns, reported to the client as
     * read-only alongside the SQL-filled ones. Row-relative names
     * ({@code years[].fiscalYearDays}) apply to every row of their table.
     */
    List<String> derivedFields();

    /**
     * Computes this page's derived values in place.
     *
     * @param context PostgreSQL-side data, for derivations that need it. May
     *                have a {@code null} company.
     */
    void derive(T form, ProjectPlanSqlContext context);
}
