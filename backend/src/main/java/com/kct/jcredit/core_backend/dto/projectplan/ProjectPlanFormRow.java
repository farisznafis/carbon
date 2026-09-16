package com.kct.jcredit.core_backend.dto.projectplan;

/**
 * Marks one row of a repeating table on a project-plan page.
 *
 * <p>The official form is mostly tables — 4.排出削減計画 has one row per 年度,
 * 11.モニタリング計画 one per モニタリング項目 — so most pages hold a
 * {@code List<X extends ProjectPlanFormRow>} rather than a flat set of fields.
 *
 * <p>The marker earns its keep by letting the framework find those lists
 * without a naming convention: {@link com.kct.jcredit.core_backend.service.projectplan.FormDefinition}
 * walks a page's declared fields for lists of rows so that row-level
 * {@code @RequiredIf} rules reach the client, and the DTO stays the single
 * place either is declared.
 *
 * <p>Implementations are nested {@code @DynamoDbBean}s. Rows live inside their
 * page's item, never as items of their own — a page is saved only when it
 * validates in full, and one item per row would turn that single write into N.
 */
public interface ProjectPlanFormRow {
}
