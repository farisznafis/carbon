# Row model for the repeating pages — proposal

The gap analysis (`projectplan-form-gap-analysis.md`) found that 210 of the
spec's 331 fields belong to tables that repeat, and that the DTOs hold one row
each. This is the design for fixing that, and the reasoning behind each choice.

Nothing here is implemented yet.

---

## Decision 1 — rows live in the page item, not in items of their own

**Recommended: one DynamoDB item per page, with `List<Row>` as an attribute.**

```java
@DynamoDbBean
public class EmissionReductionPlanRequest extends ProjectPlanFormBase {

    /** 認証対象期間 and the other per-page values stay flat. */
    private String certTargetPeriodStart;
    private String certTargetPeriodEnd;

    /** 4.1 の年度表。1 行 = 1 年度。 */
    @Valid
    @NotEmpty
    private List<EmissionReductionYearRow> years;

    /** 合計 columns — derived from the rows, never typed. */
    private BigDecimal baselineEmissionsTotal;
}
```

### Why not one item per row

The obvious alternative is `SK = SECTION#M1#ROW#0001`, queried by prefix. It is
rejected for one reason that outweighs the rest.

**Page atomicity is the invariant this whole feature rests on.** A page is
stored only when it validates in full; that is what lets `/progress` be trusted,
which is what makes 続きから land on the right page. One item per row turns a
page save into N writes. Making that atomic needs `TransactWriteItems`, and
without it a page can be half-stored — at which point progress is lying, and the
operator resumes into a page that is partly saved with no way to tell.

The supporting reasons:

- `ProjectPlanFormRepository.find(type, pk, sk)` / `save(type, item)` need **no
  change at all**. Rows are just an attribute of a bean the enhanced client
  already maps.
- Deleting a row becomes an edit to a list, not a delete-plus-renumber across
  items.

### The item-size limit does not bite

DynamoDB caps an item at 400 KB. Worst realistic row, taken from the existing
`@Size` caps on 11.モニタリング計画 (50 + 500 + 50 + 200 + 1000 + 50 + …), is
about **2.5 KB**. That is roughly **160 rows** before the limit is anywhere near.
A monitoring plan has 5–30 items; 4.排出削減計画 has 38 rows of numbers.

Worth stating so nobody re-litigates it later: if a page ever needs hundreds of
rows, this decision has to be revisited. None of the 35 does.

---

## Decision 2 — fix the decomposition, don't just add rows

This is the more important half, and it is easy to miss.

11.モニタリング計画 is **one table**:

| モニタリング項目 | モニタリング方法 | プロジェクト計画での想定 | 備考 |
|---|---|---|---|
| 記号 / 定義 / 単位 | 分類 / 概要 / 頻度 | 想定値 / 根拠 | |

We split it into six pages — `monitoring-item-plan`, `monitoring-method-plan`,
`expected-project-plan` and their `-coefficient` twins. That split is along
**column groups**. The form's unit is the **row**.

So the bug is not only "each page holds one row". It is that one row of the form
is scattered across three items with nothing to say which 定義 belongs to which
分類.

**Merge them.** Six pages become two — 活動量 and 係数 — each holding
`List<MonitoringPlanRow>` with all nine columns. The unjoinable-rows problem
then does not need solving; it stops existing.

The same test applies elsewhere: **if two pages are always filled from the same
row of the sheet, they are one page.** Section 10's eight pages survive it —
10.1, 10.2(1), 10.2(2), 10.3(1) and so on really are separate blocks in the
sheet, each with its own table.

Page count after: **35 → 31**.

---

## Decision 3 — keep rows and 方法論 on separate axes

Two different kinds of repetition, and conflating them would be a mess.

| Repetition | Where it lives |
|---|---|
| rows within a table | a `List` attribute inside the item |
| 方法論 ①–⑤ | a separate item, `SK = SECTION#M2` |

The sort-key suffix already exists (`FormDefinition.sk`), and the registry
already refuses `perMethodology` until the schema can hold more than one
methodology. Nothing about the row model needs to change when that is unblocked:
each methodology gets its own item, each item has its own rows.

The alternative — nesting methodology inside the row list — would mean loading
all five methodologies to read one, and would break the per-methodology progress
that `/progress` already reports per sort key.

**Still blocked on:** `project` stores one 方法論 in three NOT NULL columns. A
`project_methodology` table is the prerequisite, and it is a schema decision, not
a code one.

---

## Decision 4 — derive what the sheet derives

4.排出削減計画 does not ask the operator for 38 rows. The sheet computes them:
年度開始日, 年度終了日, 年度内日数 and the 年度 sequence itself all follow from
認証対象期間 (2013 → 2050 in both workbooks).

So the server should **generate the row skeleton** from 認証対象期間 and let the
operator fill only 新規会員数 / 累計会員数 / 延べ会員数 / ベースライン排出量 /
プロジェクト実施後排出量. The same applies to the 合計 columns, which are sums,
and to the sheet's rule that 合計 is always t-CO2 whatever unit the rows use.

This is the existing `SqlField` mechanism doing slightly more: today it fills
single values, here it would seed a list. Autofill already runs on every read and
PostgreSQL is already authoritative, so the shape fits.

**Consequence worth naming:** derived columns must be read-only, and
`readOnlyFields` currently names flat fields only. See below.

---

## What this costs

Honest list of what breaks and needs work, not just the happy path.

| Area | Change |
|---|---|
| `validate()` | Violation paths become `years[0].newMembersTotal`. `JsonFieldNameResolver.resolve` takes a class and a property name; it has no notion of an index and will not resolve that. Needs to strip and re-apply the index. |
| Frontend error mapping | `applyBackendValidationErrors` maps a bare name to `page.formKey + name`. Row errors need the index carried through to `formKey.years.0.field`. |
| `readOnlyFields` / `conditionalFields` | Both are flat name lists. Rows need row-relative names, e.g. `years[].yearStartDate`. |
| Frontend rendering | `FieldConfig` has no table type. A repeating-row renderer with add/remove is the single largest piece of work here. |
| Stored dev data | Existing items have flat attributes and will not map to a `List`. There is no production data; dev items need re-entering. |
| Page slugs | Merging six pages into two retires four slugs. They are URLs and DynamoDB sort keys, so it is a migration, not a rename. |

---

## Suggested sequence

Do **one** page end to end before touching the rest. The framework changes above
are all discovered properly only by shipping one.

1. **4.排出削減計画 first.** Its rows are derivable (Decision 4), so the first
   version needs the least new UI — a mostly read-only table. It is also the page
   that surfaced this, and the one where "the form cannot be filled in at all" is
   most obviously true.
2. Then 11.モニタリング計画, because it also carries the merge (Decision 2) and
   is the hardest UI. Doing it second means the table renderer is written against
   a real case rather than guessed at.
3. Then section 10's eight pages, which by then are mechanical.

Not recommended: changing all fifteen at once. The validation, error-mapping and
read-only plumbing above will each need a second pass, and finding that out
across fifteen pages costs three times what finding it out across one does.

---

## What I am not sure about

- ~~Whether `monitoring-plan` (page 31) duplicates part of 11 or belongs to
  12.x.~~ **Answered in stage C**: it is 11（３）モニタリングデータの収集方法, and
  it holds only that section's three header fields — the table under them is
  still missing.
- Whether 12.1 / 12.2 / 12.3 are row tables. The gap analysis assumed so from
  their column headers, but the rows were not counted.
- Whether the 合計 rounding rule (小数点以下第１位, per 10.1 ※4) should be applied
  on write or only on display.

---

## Stages

Six. A must come first; B–D are the pages themselves; E and F are independent
of the rest and can slot in anywhere.

### A. Framework plumbing — no page changes yet ✅ done

Everything a row table needs before the first one exists. Doing this while
changing a page as well would tangle two kinds of failure together.

1. ✅ `JsonFieldNameResolver` understands indexed paths
   (`years[0].newMembersTotal`). It now descends into the `List`'s element type
   instead of into `List` itself, so a row field keeps its `@JsonProperty` name
   through the index.
2. ✅ `applyBackendValidationErrors` (frontend) carries the index through:
   `years[1].symbol` becomes `formKey.years.1.symbol`. Left in bracket form,
   `setError` invents a field literally called `years[1]` and the cell the
   operator has to fix is never marked. Table and column are renamed
   independently.
3. ✅ `readOnlyFields` / `conditionalFields` take row-relative names
   (`years[].yearStartDate`, marker `FormDefinition.ROW_MARKER`). Row-level
   `@RequiredIf` is collected off the row type and reported prefixed; the
   server already enforces it per row because `@Valid` on the list runs
   class-level constraints on every element.
4. ✅ `FieldConfig` gained `type: "table"` (+ `columns` / `minRows` / `maxRows`
   / `fixedRows` / `addRowLabel`), and `PlanTableField` renders the rows with
   add / remove. The control switch was split out of `DynamicFormField` as
   `FieldControl` so a cell and a labelled field share one implementation.

New: `ProjectPlanFormRow`, a marker interface on row DTOs. It is how the
framework finds a page's tables without a naming convention.

**Done:** 9 backend tests (`RowTableFrameworkTest`) + 7 frontend
(`PlanTableField.test.tsx`) + 2 (`backendValidationErrors.test.ts`). The
throwaway page round-trips through both DynamoDB's `TableSchema` and Jackson,
and reports `rows[1].rowCode` for the second row alone. Existing single-row
pages are untouched: 153 backend projectplan/util/validation tests and the 33
project-registration frontend tests all pass.

Two things worth knowing before Stage B builds on this:

- **A row's required badge lives in the column header, not the cell.** So for a
  column under a 有/無 rule the badge is dropped and the rule becomes `hideWhen`
  on the opposite value — matching the page-level behaviour of leaving a field
  visible while its toggle is still unanswered. The server stays the authority
  on which rows actually need it.
- **`GenericFormFields` skips `type: "table"` outright.** It draws one control
  per config and has nowhere to put a row index; falling through to its text
  input would bind a string to an array.

### B. First page — 4.排出削減計画 ✅ done

5. ✅ `EmissionReductionYearRow` + `List<EmissionReductionYearRow> years`.
6. ✅ Server derives the 年度 skeleton and marks it read-only.
7. ✅ 合計 columns computed from the rows, always t-CO2.
8. ✅ Table renderer wired to it.

**One correction to item 6 as written above.** The 年度 range is **not** derived
from 認証対象期間 — both workbooks fix the table at 2013–2050, 38 rows, whatever
the project's period is. What 認証対象期間 derives is 年度内の認証申請期間日数,
the overlap of the period with each fiscal year. 年度開始日 / 年度終了日 /
年度内日数 are pure calendar (4月1日 – 翌3月31日, 366 when the year spans a leap
February).

New framework piece: **`FormDerivation`**, alongside `SqlField`. A `SqlField`
copies something PostgreSQL already holds; a derivation produces something that
exists nowhere until it is calculated. It runs on every read and every save, and
overwrites what the client sent — which is what makes it safe to send a 合計 to
the client at all. `FormDefinition` gained an eighth component for it plus a
seven-argument constructor, so the other 34 registry entries did not change.

Verified against the workbooks rather than assumed, and the test carries the
numbers:

| Rule | Evidence |
|---|---|
| 38 rows, 2013–2050 | rows 7–44 of both workbooks |
| 年度内日数 366 on a leap-February year | FY2015 → 366, FY2027 → 366 |
| 認証申請期間日数 = overlap | solar period 2025-10-01 → 2050-10-01 gives 182 / 365 / 184, matching U19, U24, U44 |
| 合計 converts to t-CO2 | rows sum to 43,105,545.86 kg; sheet reads 43105.5 |
| 総量 subtracts the **rounded** totals | 43105.5 − 7788.5 = 35317.0, and the sheet reads 35317 — the raw difference is 35317.0822 |
| 年平均 = 総量 ÷ 認証対象期間 | 35317 ÷ 25.0 = 1412.68, matching F47 |

Three deliberate departures from what was there before:

- **The page lost all six of its `SqlField`s.** They copied single aggregates off
  `project` (新規/累計/延べ会員数, the two emission figures, 排出削減量の総量)
  into what the sheet holds as 38 fiscal-year rows. There is no year to put a
  project-wide total in. 排出削減量の総量 is now summed from the rows, so where
  it and `project.emission_reduction` disagree, the rows win.
- **`unitEmission` is now restricted to `t-CO2` / `kg-CO2`.** The pulldown sheet
  lists those separately from `t-CO2/年` / `kg-CO2/年`, and only an absolute
  figure can be summed into 合計. The frontend offered only the `/年` pair, so
  it offered the wrong group; `ABSOLUTE_EMISSION_UNITS` was added for it.
- **`emissionReductionAnnualAverage` is new.** 排出削減量の年平均 is in the sheet
  (row 47) and was in no DTO.

Blank rows are seeded with zeros, because that is what the blank official format
ships — rows 7–44 of 別紙7 already read 0. Leaving them null would make a page
of 38 required rows unsaveable until every year is typed, for a plan that
usually concerns a handful of them.

年度開始日 / 年度終了日 / 年度内日数 are stored and returned but **not shown**.
The sheet carries them (columns V–X) because Excel needs them for its formulas;
they are the same calendar for every project in every year, so on screen they
are three columns of noise across 38 rows that nobody reads and nobody can
change.

Tests: `EmissionReductionPlanDerivationTest` (13). Whole backend
projectplan/util/validation set: 166 pass.

### C. 11.モニタリング計画 — the merge ✅ done

9. ✅ Six pages collapse to two (活動量 / 係数), each with
   `List<MonitoringPlanRow>` carrying all nine columns.
10. ✅ All six slugs retire, replaced by `monitoring-plan-activity` and
    `monitoring-plan-coefficient`. They are URLs and DynamoDB sort keys, so
    this is a migration, not a rename. The wizard is 35 pages → **31**.

Naming: the plan said four slugs retire and two survive, but the two survivors
would have been `monitoring-item-plan` and `monitoring-item-plan-coefficient`,
whose names describe one column group of the three they now hold. The migration
costs the same either way, so all six went.

**（３） is a third table, and 11 has three pages now.** The existing
`monitoring-plan` page turns out to be 11（３）モニタリングデータの収集方法 — that
resolves the "does it duplicate 12.x?" question in the open list below. It sits
after the other two in sheet order, and its display name now says so.

Four things the workbooks settled that the DTOs had wrong:

| Finding | Evidence |
|---|---|
| **（２）係数 uses 分類Ⅰ・Ⅱ・Ⅲ**, not Ａ・Ｂ・Ｃ | the sheet carries a separate ※1 note under each table; the solar example reads "B" on 活動量 and "Ⅲ" on 係数 |
| `@JapaneseText` on 分類 rejected **every value the coefficient table offers** | its pattern has no Roman numerals (U+2160–). Both old DTOs carried it |
| 根拠 `@Size(max = 200)` was too tight | the solar example's longest is already **194** characters. Raised to 1000 |
| One モニタリング項目 may take **several rows** | 「複数のモニタリング方法を用いる…想定される手法を全て記載すること」. ELPV appears with 分類Ｂ then 分類Ｃ, the second row leaving 定義/単位/根拠/備考 blank |

So 記号 is required on every row and a continuation row repeats it, while 定義,
単位, 根拠 and 備考 became optional. Repeating the 記号 rather than leaving it
blank is what keeps a row self-describing when read on its own — which was the
whole problem with the old three-page split.

**A pre-existing defect fell out of this.** Renumbering the registry needed a
contiguity guard, and the new guard immediately failed: `applicable-conditions`
and `project-change-information` both had `order = 4`, so every page after them
was numbered one below its wizard step. `all()` sorts by order, so two pages
sharing one had no defined relative order. Fixed; orders now run 2–32 and match
the wizard's step ids exactly.

**Still missing after this stage:** （３） has a table of its own — モニタリング項目
/ 必要情報 / 収集する資料 / データ形態 / モニタリング対象者 / 収集時期 / 収集方法,
at sheet rows 31+ — and `MonitoringPlanRequest` holds only （３）'s three header
fields. That is a newly identified gap, not part of this stage; it belongs with
stage D or F.

### D. Section 10 — eight pages

11. A row list on each of 10.1, 10.2(1), 10.2(2), 10.3(1), 10.3(2), 10.4,
    10.5(1), 10.5(2). No merging — each really is its own block in the sheet.

Mechanical by this point.

### E. 方法論 ①–⑤ — blocked on schema, independent of A–D

12. `project_methodology` table; `project` currently holds one methodology in
    three NOT NULL columns.
13. Remove the startup guard in `ProjectPlanFormRegistry`, set `perMethodology`
    on the nine repeating sections.
14. Also fixes the `method_number UNIQUE` question, which only exists because
    the methodology lives on `project`.

### F. Loose ends — any time

15. 認証権限保有主体変更届 has no page at all.
16. 影響度 becomes numeric 0–100 (it is compared against 1% in 10.3(2)).
17. 分類 becomes a real choice — **Ａ／Ｂ／Ｃ on 11（１）活動量 but Ⅰ／Ⅱ／Ⅲ on
    11（２）係数**, which stage C confirmed from the sheet's two ※1 notes. The
    活動量 value also selects whether 12.1 or 12.2 applies. The two tables share
    one `MonitoringPlanRow` today, so this either splits the row type or keys
    the vocabulary off the page.
20. 11（３） has a table of its own (モニタリング項目 / 必要情報 / 収集する資料 /
    データ形態 / モニタリング対象者 / 収集時期 / 収集方法) that no DTO holds.
    Found in stage C.
18. Spec typos → DTO / slug / sort-key renames. Cosmetic, but a migration.
19. The three open questions in the section above need a human read of the
    workbook.
