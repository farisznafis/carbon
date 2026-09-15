package com.kct.jcredit.core_backend.service.projectplan;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanFormBase;

/**
 * The 合計 row under 10.2's 排出活動 tables.
 *
 * <p>The sheet totals a column of the table, and its ※1 warns
 * 「行を追加して記入した場合には、合計の参照範囲を確認すること」 — a caution that
 * only exists because in Excel the total can fall out of step with the rows.
 * Here it cannot: the server recomputes it from the rows on every read and
 * every save.
 *
 * <p>Both 10.2 pages use this; 10.2（２）passes a second column for 影響度.
 *
 * @param <T> the page type
 * @param <R> its row type
 */
public final class IncidentalActivityTotalsDerivation<T extends ProjectPlanFormBase, R>
        implements FormDerivation<T> {

    private final Function<T, List<R>> rows;
    private final List<Column<T, R>> columns;

    private IncidentalActivityTotalsDerivation(
            Function<T, List<R>> rows, List<Column<T, R>> columns) {
        this.rows = rows;
        this.columns = columns;
    }

    @SafeVarargs
    public static <T extends ProjectPlanFormBase, R> IncidentalActivityTotalsDerivation<T, R> of(
            Function<T, List<R>> rows, Column<T, R>... columns) {
        return new IncidentalActivityTotalsDerivation<>(rows, List.of(columns));
    }

    /**
     * One totalled column: how to read it off a row, and where the sum goes.
     *
     * @param jsonName the total's JSON name, reported read-only
     * @param read     the row value being summed
     * @param write    where the sum is stored on the page
     */
    public record Column<T, R>(
            String jsonName, Function<R, BigDecimal> read, BiConsumer<T, BigDecimal> write) {
    }

    @Override
    public List<String> derivedFields() {
        return columns.stream().map(Column::jsonName).toList();
    }

    @Override
    public void derive(T form, ProjectPlanSqlContext context) {
        List<R> present = rows.apply(form);
        for (Column<T, R> column : columns) {
            column.write().accept(form, sum(present, column.read()));
        }
    }

    /**
     * The column's sum, or {@code null} when the page has no rows yet.
     *
     * <p>Zero would be a claim — that the operator listed no 付随的な排出活動 and
     * their total really is nothing. A page nobody has filled in has no total.
     */
    private static <R> BigDecimal sum(List<R> rows, Function<R, BigDecimal> read) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (R row : rows) {
            if (row == null) {
                continue;
            }
            BigDecimal value = read.apply(row);
            if (value != null) {
                // A cleared cell counts as zero, as a blank cell does in SUM().
                total = total.add(value);
            }
        }
        return total;
    }
}
