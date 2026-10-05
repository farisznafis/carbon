# Row model for the repeating pages — proposal

The gap analysis (`projectplan-form-gap-analysis.md`) found that 210 of the
spec's 331 fields belong to tables that repeat, and that the DTOs hold one row
each. This is the design for fixing that, and the reasoning behind each choice.

**Status (2026-08-21): stages A, B, C, D, F and G are implemented; E was
decided against.** The design below is kept as written so the reasoning stays
readable; each stage's section records what actually happened, including where
the plan turned out to be wrong. What is still open is listed at the end.

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
  it already matches the blank format in full. （３）is free-form — a note, a
  モニタリング項目 list and a フロー図 — so there is no table under it to add.
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

**Correction — （３） needs nothing.** This stage first recorded 「（３）has a
table of its own that no DTO holds」, from the solar example's rows 31–40
(モニタリング項目 / 必要情報 / 収集する資料 / データ形態 / モニタリング対象者 /
収集時期 / 収集方法). Checking the blank format instead: **11（３）ends at row
21.** It is a heading, the note 「各削減活動におけるモニタリングデータを運営・
管理者が収集する方法をフロー図等を用いて説明すること」, and a モニタリング項目
label — then open space. The table is something the sample author drew inside
that free-form space, not a structure the form defines.

`MonitoringPlanRequest` already matches the blank format field for field:
モニタリング項目 (a comma-joined symbol list), the explanation, and the diagram.
Nothing to add.

The lesson is worth keeping: **the example shows one way to fill the form, the
blank format is the form.** Reading structure out of the sample invents
requirements J-Credit never set.

### D. Section 10 — eight pages ✅ done

11. ✅ A row list on **seven** of the eight. No merging — each really is its own
    block in the sheet.

Two row shapes cover the section:

- **`FormulaSymbolRow`** — 記号 / 定義 / 単位 / 想定値, the table under every 式.
  Serves 10.3(1), 10.3(2), 10.4(2), 10.5(1), 10.5(2). Each of those pages held
  one symbol, and every formula in either workbook has at least two: the solar
  example's 式 5 is `EMPJ,S,control = ELPJ,control × CEFelectricity,t`.
- **`BaselineIncidentalActivityRow`** / **`PostImplementIncidentalActivityRow`**
  — 10.2's 排出活動 tables, with the 合計 beneath each now derived.

**10.1 is deliberately left flat**, against the stage as written. Its table is
`ER` / `EMBL` / `EMPJ` — three rows the sheet prints itself, symbol and 定義
included, with only 単位 and 数値 to fill. A row list there would replace three
named fields with three rows that can never be anything else, and would lose the
names the rest of the code refers to them by.

Two more bugs of the character-set family, both found by writing tests with the
workbooks' own values:

| Bug | Evidence |
|---|---|
| **`evaluationFormula` carried `@JapaneseText`**, which allows neither `=` nor `×` — so every formula in either workbook was rejected by the page meant to hold it | `ER=EMBL - EMPJ` (blank, 式 1), `EMPJ,S,control=ELPJ,control×CEFelectricity,t` (solar, 式 5) |
| **影響度 was free text up to 1000 characters**, which is how a value above 100 got in | it is a share of 10.1's 排出削減量 (※1) and 10.3(2) applies to activities 「影響度が１％以上」. Now `BigDecimal`, 0–100 |

That second one was pulled forward from stage F item 16: the 影響度合計値 the
sheet shows underneath cannot be summed from free text, so stage D's derived
total depended on it. Note the unit — **percentage points, 0–100**. The
spreadsheet cell holds the raw ratio (the solar example stores 0.0407) and Excel
renders it as 4.07% because the cell is percent-formatted; the operator reads
and types the percentage, so that is what we store.

`FormDerivation` generalised slightly: `IncidentalActivityTotalsDerivation` takes
the row list and one `Column` per total, so 10.2(1) declares one and 10.2(2)
declares two. An empty table gives a **null** total rather than zero — zero
would be a claim that the operator listed no 付随的な排出活動.

**Still missing:** the sheet allows several 式 blocks per sub-section, each with
its own 式番号 and symbol table — the solar example's 10.3(2) has 式 4, 4, 5 and
6. We hold one 式 per page, which is what the blank format shows. Fixing it means
a table nested inside a table row, which the renderer does not do.

Tests: `SectionTenRowTest` (13). Backend set: 182 pass.

### E. 方法論 ①–⑤ — ❌ decided against, not building

**One project applies one methodology on this platform.** That is the answer,
given 2026-08-21, and it closes this stage rather than deferring it.

What the form asks for, for the record: the blank workbook has 59 sheets, **45
of them per-methodology** — nine sections × five — and `登録申請書` provides five
slots at rows 44, 46, 48, 50 and 52. A project using one methodology fills ①
and leaves ②–⑤ blank, which the form permits, so holding one costs no
compliance.

What it would have taken, for the record:

- a `project_methodology` table — `project` holds the methodology in three
  NOT NULL columns and monitoring, credit calculation and the participation
  PDFs all read it from there;
- an answer to where `method_number UNIQUE` belongs once methodology leaves
  `project`;
- a wizard that navigates two axes. **24 of our 31 pages sit in those nine
  sections**, so ①–⑤ would take the wizard to 7 + 24 × 5 = **127 pages** — a
  flat step list cannot carry that, and rebuilding the navigation is work on
  the scale of stage A.

The `perMethodology` flag, the `#M2` sort-key suffix and the startup guard all
stay. The flag is always false by decision; the guard is what stops that from
being forgotten, and it needs something to guard.

### F. Loose ends ✅ done

15. ✅ **認証権限保有主体変更届 now has a page**, at wizard step 7 — where the
    workbook puts the sheet, after 表紙 and before 1.プロジェクト概要. Two blocks:
    プロジェクト登録情報 (both fields autofilled) and 所有者変更内容.
16. ✅ 影響度 numeric — done in stage D.
17. ✅ **分類 is a controlled choice, and the two tables do not share one.**
18. ✅ The three misspelled JSON names are aligned.

**New framework concept: an `optional` page.** はじめに rows 44–51 say to unhide
認証権限保有主体変更届, fill it, and pick 認証権限保有主体変更 as the 申請区分 —
so it is filed only when the owner changes. Registered as a normal page it would
have broken two things at once: a plan that legitimately never files it could
never read complete, and 続きから would park every operator on a form they must
not fill in. So `FormDefinition` gained `optional`; such a page is listed and
reachable, but left out of `totalForms` / `completedForms` and skipped by
`nextIncompleteSlug`.

Other pages are plausibly in the same category — 計画変更届 (only for 変更),
代表以外のプログラム型運営・管理者 (only when there are others), the 参考様式
誓約書. Not changed: flipping them alters progress for plans already in flight,
and that is a product call.

**分類 split.** `MonitoringPlanRow` is now abstract and holds the eight shared
columns; `MonitoringActivityPlanRow` and `MonitoringCoefficientPlanRow` each add
their own 分類 with their own permitted values — `A|B|C` against `Ⅰ|Ⅱ|Ⅲ`.
Values are what the official example stores: half-width letters, and the Roman
numeral characters. Sharing one free-text field meant 分類Ⅲ could be filed
against 活動量 and nothing said otherwise.

**The three misspellings** were `energyManagFactoryNumber`,
`opeManagStructure` and `infoCollManagMethod` — all "Manage" truncated to
"Manag" in the spec. Worth doing not for tidiness but because
`OtherOperatorProjectsRequest` and `ProjectApplicationRequest` spelled the
*same* field differently, and the frontend carried three rename entries to
bridge it. Those entries are gone.

### Open questions, answered

- **Are 12.1 / 12.2 / 12.3 row tables?** 12.1（１）is — **two** of them,
  ①特定計量器の場合 and ②特定計量器以外の計量器の場合, each 4 columns
  (モニタリング項目 / 計量器の種類 / モニタリングポイント / 検定の有効期限 or
  校正方法). `measure-specified-instrument` and `measure-other-instruments` hold
  one row each, so **this is the same defect as section 10 and is still open.**
  12.1（２）, 12.2 and 12.3 are free-form: a モニタリング項目 label and space for
  a description or diagram. **Both tables now hold rows** — see below.
- **Should the 合計 rounding rule apply on write or on display?** On write.
  10.1 ※4 reads 「算定結果は小数点以下第２位を四捨五入し小数点以下第１位まで
  **記載すること**」 — it governs the recorded figure. Stage B already rounds on
  write, and that reproduced the solar example to the digit.

### G. 12.1（１）計量器の概要 ✅ done

Not part of the original six stages — it came out of answering F's open
question. The sub-section is two tables, ①特定計量器の場合 and
②特定計量器以外の計量器の場合, sharing three columns and differing in the
fourth (検定の有効期限 against 計量器の校正方法の説明). Each page held one 計量器
while the solar example lists two in each.

`MeasureInstrumentRow` is abstract and holds the three shared columns;
`SpecifiedInstrumentRow` and `OtherInstrumentRow` add their own — the same
shape as the 分類 split in F.

Three things the example settled:

- **計量器の校正方法の説明 is optional.** The example fills it on the first row
  and leaves it blank on the second; required, as it was, the page would have
  refused a table the official example itself contains.
- **検定の有効期限 is a duration, not a date** — 「10年間」. No date format is
  imposed on it.
- **モニタリングポイント may be half- or full-width.** The example writes
  `M1` on one row and `Ｍ2` on the next, and its ※1 says the value must match
  a number in 12.1（２）.

Also renamed 特定計量器's diagram field from `methodCollDescriptionImg` to
`monitoringPointDiagram`, which is what it holds and what the sibling page
already called it. Two more frontend rename entries went with it — and the
column names inside a row now have to match the DTO exactly, because
`mapPlanPagePayload` renames top-level keys only, never inside a row.

Tests: `MeasureInstrumentRowTest` (5). Backend set: 196 pass.

**Left alone:** both pages carry their own モニタリングポイント図示 while the
sheet has one 12.1（２）for the whole sub-section. Which page should own it is a
presentation question, not a data one.

### Still open

- Several 式 blocks per sub-section in section 10 (stage D).
- Making 分類Ｂ / Ｃ actually select whether 12.1 or 12.2 applies — the values
  are constrained now, but nothing keys off them yet.

### F. Loose ends — original list

15. 認証権限保有主体変更届 has no page at all.
16. ~~影響度 becomes numeric 0–100~~ — **done in stage D**, because the
    影響度合計値 that stage derives cannot be summed from free text.
17. 分類 becomes a real choice — **Ａ／Ｂ／Ｃ on 11（１）活動量 but Ⅰ／Ⅱ／Ⅲ on
    11（２）係数**, which stage C confirmed from the sheet's two ※1 notes. The
    活動量 value also selects whether 12.1 or 12.2 applies. The two tables share
    one `MonitoringPlanRow` today, so this either splits the row type or keys
    the vocabulary off the page.
20. 10.1's EMBL and EMPJ are stated by the sheet to be sums — ※2 「10.5の
    ベースライン排出量で算定した全ての排出量の総和」, ※3 the same for 10.3 —
    so they could be derived across pages rather than typed. Found in stage D;
    not done, because it is the first derivation that would read another page's
    stored item.
18. Spec typos → DTO / slug / sort-key renames. Cosmetic, but a migration.
19. The three open questions in the section above need a human read of the
    workbook.
