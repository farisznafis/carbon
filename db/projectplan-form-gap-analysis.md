# プロジェクト計画書 — form vs implementation gap analysis

Compared the registry's 35 pages against the two official workbooks now in `db/`:

- `別紙7.プロジェクト計画書フォーマット.xlsx` — 59 sheets, blank format
- `sakuseirei_keikaku_solar_program.xlsx` — 60 sheets, filled solar example

Read directly from the files (sheet names, cell text, row ranges). `db/projectplan.xlsx`
is analysed separately in §7; it is a single flat `Sheet1` whose columns cannot
express repetition at all, which is the root of most of what follows.

**No code has been changed for this document.**

---

## 1. Sheet ↔ page mapping

One row per unique sheet. The ②–⑤ copies are omitted; see §2.

| Official sheet | Our page(s) | Status |
|---|---|---|
| 登録申請書 | `application` | ok |
| 代表以外のプログラム型運営・管理者 | `other-operator-projects` | ok |
| (参考様式)適用条件（環境社会配慮）に係る誓約書 | `applicable-conditions` | ok |
| 【HP公開】計画変更届 | `project-change-information` | ok |
| 認証権限保有主体変更届 | — | **missing** |
| 【HP公開】プロジェクト計画書表紙 | `project-plan-cover` | ok |
| 【HP公開】1.プロジェクト概要 | `project-overview` | ok (1.1–1.6 in one page) |
| 【HP公開】2.適用する方法論 | `applicable-methodology` | ok (2.1–2.3) |
| 【HP公開】3.プログラム型プロジェクトの運営・管理 | `program-type-methodology` | ok (3.1–3.2) |
| 【HP公開】4.排出削減計画 | `emission-reduction-plan` | **shape wrong** — §3 |
| 【HP公開】5.モニタリング・算定方法 | `monitoring-calculation` | ok (5.1–5.2) |
| 【HP公開】6-7.データ管理・特記事項 | `data-management` | ok (6.1–6.2, 7.1–7.3) |
| 【HP公開】8.設備情報の収集・確認方法 | `equipment-verification-before-update` / `-before-new` / `-after` | ok (8.1, 8.2) |
| 【HP公開】9.追加性の評価 | `additional-assessment-method`, `obtaining-additional-assessment` | ok (9.1, 9.2) |
| 【HP公開】10.排出削減量の算定方法 | 8 pages | see §4 |
| 【HP公開】11.モニタリング計画 | 6 pages | **shape wrong** — §5 |
| 【HP公開】12.モニタリング方法等に関する説明 | `measure-specified-instrument`, `measure-other-instruments`, `monitoring-method-explanation`, `coefficient-measure-explanation`, `monitoring-plan` | ok (12.1–12.3), but see §5 |

### 1.1 認証権限保有主体変更届 has no page

Present in **both** workbooks. Sub-titled 所有者を変更する方のみ提出, so it is
conditional, not always required — but there is no way to file it at all today.

---

## 2. 方法論 ①–⑤ — nine sections repeat five times

Confirmed from the sheet list. These nine repeat once per methodology:

`2.適用する方法論` · `3.プログラム型…運営・管理` · `4.排出削減計画` ·
`6-7.データ管理・特記事項` · `8.設備情報の収集・確認方法` · `9.追加性の評価` ·
`10.排出削減量の算定方法` · `11.モニタリング計画` · `12.モニタリング方法等に関する説明`

Singletons: 登録申請書, 代表以外…, 誓約書, 計画変更届, 認証権限保有主体変更届,
表紙, `1.プロジェクト概要`, `5.モニタリング・算定方法`.

That is 8 singleton sheets + 9 × 5 = **53**, plus 項目取得シート / 更新履歴 /
はじめに and the three 【削除不可】 sheets.

**Status: not a gap — decided 2026-08-21.** One project applies one methodology
on this platform. `project` stores that one (`method_category` /
`method_number` / `method_name`, all NOT NULL), and `ProjectPlanFormRegistry`
throws at startup if a page sets `perMethodology`, so the decision cannot be
forgotten by accident.

This costs no compliance: the form provides ①–⑤, and a project using one fills
① and leaves ②–⑤ blank, which the form permits. A 事業者 running two
methodologies files two projects.

### 2.1 蓄電池あり／なし is example-specific, not part of the form

The earlier plan recorded 「4.排出削減計画 ① additionally splits 蓄電池あり /
蓄電池なし」 as a form feature. It is not:

- `別紙7` (the blank format) has exactly five: `4.排出削減計画_方法論①`
  through `_方法論⑤`, one per methodology and nothing more
- the solar example has **six**: it renamed ① to `① (蓄電池あり)`, inserted
  `②(蓄電池なし)`, and still carries plain ②③④⑤

So the sample author spent an extra slot on a battery variant. The form itself
has one 4.排出削減計画 per methodology. Nothing to build.

---

## 3. 4.排出削減計画 is a table of fiscal years, we store one row

The sheet (4.1 プログラム型プロジェクトの全体計画) is one row per 年度.
Both workbooks carry **38 rows, 2013 → 2050**, in sheet rows 7–44, with columns:

年度 · 新規会員数（件） · 認証対象となる累計会員数（件） · 延べ会員数（件） ·
ベースライン排出量 + 単位 · プロジェクト実施後排出量 + 単位

plus 年度内の認証申請期間日数 / 年度開始日 / 年度終了日 / 年度内日数, which the
sheet derives and we hold none of.

`EmissionReductionPlanRequest` has:

```java
private Integer year;                 // one year, not a list
private Integer newMembersTotal;      // one value
private Integer cumulativeMembersTotal;
private Integer membersTotal;
private BigDecimal baselineEmissions;
private BigDecimal afterImplEmissions;
...
private BigDecimal baselineEmissionsTotal;   // then jumps straight to totals
private BigDecimal emissionReductionTotal;
```

The totals exist but the rows they total do not. **A project cannot record its
emission plan at all** — only a single year of it.

### 3.1 The unit note is not implemented

The sheet carries: 「シート内、「単位」で選択した単位と揃えてください。なお最終的な
合計は選択内容に関わらず t-CO2 となります。」

Per-row units may be kg-CO2 (the solar example uses kg-CO2 throughout), **but the
total is always t-CO2**. No conversion exists anywhere in the code;
`baselineEmissionsTotal` is a bare `BigDecimal` with no unit and no arithmetic.

---

## 4. 10.排出削減量の算定方法 — one sheet, eight of our pages

This is the sheet behind the "what are pages 20–25?" confusion. Its structure:

| Sub-section | Our page |
|---|---|
| 10.1 排出削減量 | `emission-reduction-calculation-method` |
| 10.2（１）ベースラインの付随的な排出活動 | `baseline-incidental-emission-activity` |
| 10.2（２）プロジェクト実施後の付随的な排出活動 | `post-implement-incidental-activity` |
| 10.3（１）主要排出活動 | `main-activity-project-emission` |
| 10.3（２）付随的な排出活動 | `incidental-activity-project-emission` |
| 10.4 ベースライン排出量の考え方 | `baseline-emission` |
| 10.5（１）主要排出活動 | `main-baseline-emission` |
| 10.5（２）付随的な排出活動 | `incidental-baseline-emission` |

The split itself is reasonable. Two problems inside it:

### 4.1 Every sub-table is multi-row, we store one row

10.1, 10.3(1)(2), 10.4(2), 10.5(1)(2) each carry a 記号 / 定義 / 単位 / 想定値
table — one row per symbol in the formula. Our DTOs hold **one** `symbol`,
`definition`, `unit`, `expectedValue`. A formula with three symbols cannot be
entered.

10.2(1)(2) carry 排出活動 rows plus a 合計. The sheet says so explicitly:
「行を追加して記入した場合には、合計の参照範囲を確認すること。」 — rows are
expected to be added.

### 4.2 影響度(％) is a number, and is compared numerically

10.2(2) column: 影響度(％) ※1. And 10.3(2) note reads: 「10.2(2)において、
**影響度が１％以上**であった付随的な排出活動に全てについて…」

So 影響度 drives a numeric threshold. Ours is:

```java
@NotBlank @Size(max = 1000) @JapaneseText(allowAlphanumeric = true)
private String impact;
```

Free text up to 1000 characters — which is why a value above 100 is accepted.
Its own sibling `impactTotalValue` is already `BigDecimal`; free text cannot be
totalled.

---

## 5. 11.モニタリング計画 — one table split across three pages, with no way to rejoin the rows

The sheet is a single table with grouped columns, repeated for （１）活動量 and
（２）係数:

| モニタリング項目 | モニタリング方法 | プロジェクト計画での想定 | 備考 |
|---|---|---|---|
| 記号 / 定義 / 単位 | 分類 / 概要 / 頻度 | 想定値 / 根拠 | |

We store that as **six separate pages** — `monitoring-item-plan`,
`monitoring-method-plan`, `expected-project-plan` and their `-coefficient`
twins. Each is its own DynamoDB item with its own sort key and **no row
identifier**.

So one logical row of the form is spread across three pages that cannot be
correlated afterwards: there is nothing to say which 定義 belongs to which
分類. Combined with each page holding only one row (§4.1), the sheet cannot be
reproduced.

`monitoring-plan` (page 31) also exists and overlaps this area; its place in the
sheet is unclear and needs checking against 12.x.

### 5.1 分類 is Ａ／Ｂ／Ｃ, not free text

「モニタリング・算定規程に沿って、分類Ａ・Ｂ・Ｃのいずれかの方法を選択すること。」
`classification` is a plain String. It should be a three-value choice, and 12.1
/ 12.2 are explicitly 分類Ｂ / 分類Ｃ, so the value selects which explanation
sheet applies.

---

## 6. Summary — what actually blocks a complete plan

| # | Gap | Blocking? |
|---|---|---|
| 3 | 4.排出削減計画 stores one year, form has 38 (2013–2050) | **yes** |
| 4.1 | 記号/定義/単位/想定値 tables store one row | **yes** |
| 5 | 11.モニタリング計画 rows split across pages, unjoinable | **yes** |
| 2 | 方法論 ①–⑤ repetition | yes, already blocked deliberately |
| 1.1 | 認証権限保有主体変更届 missing | only for ownership changes |
| 4.2 | 影響度 is free text, should be 0–100 numeric | data quality |
| 3.1 | unit conversion to t-CO2 for totals | data quality |
| 5.1 | 分類 should be Ａ／Ｂ／Ｃ | data quality |

The first three share one cause: **the form is built from repeating tables, and
`db/projectplan.xlsx` flattened every one of them into a single column set.**
The DTOs were generated from that spec, so they inherited the flattening. This
is the same defect as the 方法論 repetition, and it is not fixable field by
field — those pages need a row collection (`List<Row>`), which changes both the
DynamoDB item shape and the wizard UI.

Deciding the row model is the next step, and it should be decided once for all
of them rather than per page.

---

## 7. What is wrong in `db/projectplan.xlsx`, and why

331 field rows in one flat `Sheet1`, grouped into 34 tables. Columns A–U:
No · カテゴリ · テーブル名 · シート名 · カラム名 · 項目名 · 記入例 · セルの色 ·
参加企業/管理会社/SaaS · データ型 · 長さ · NULL不可 · 列制約 · 文字種チェック ·
記号使用制限 · 書式チェック · 入力制約 · 論理チェック · 備考 · SQL対象 · データチェック

### 7.1 The root cause: no column can express repetition

That column list has **no cardinality**. There is nowhere to write "this table
has one row per 年度", or "this section repeats once per 方法論 ①–⑤". Every
field is one row of the spec, and one row of the spec became one field of a DTO.

So the flattening was not a mistake anyone made while reading the form — the
spec format made it unavoidable. Every repeating table in the official workbook
came out as a single set of columns, and the generated DTOs inherited that
shape. §3, §4.1 and §5 are all the same defect showing up in three places.

**This is the field the spec is missing**, and adding it is the change that
makes the rest checkable:

| New column | Values | Meaning |
|---|---|---|
| 繰り返し単位 | `単一` / `行` / `方法論` | is this one value, a row in a table, or a per-methodology copy |
| 繰り返し範囲 | e.g. `年度 2013–2050`, `記号ごと` | what drives the repetition |

### 7.2 Duplicate column names inside one table

Five columns appear twice in `project_application`:

| Column | Spec rows |
|---|---|
| `corp_number` | 7, 61 |
| `project_number` | 40, 57 |
| `methodology` | 44, 64 |
| `project_outline` | 47, 62 |
| `registration_date` | 54, 58 |

The second occurrence of each sits in the 登録申請書 block that the official
sheet marks **転記** — values copied from elsewhere for display, not entered.
They are not fields; they should be removed from the spec, not de-duplicated.
The same block is why `operator_phone_number`, `implimentation_area` and
`cert_target_period_start` appear in `project_application` at all.

### 7.3 Two rows whose カラム名 is not a column name

| Spec row | Table | Value |
|---|---|---|
| 46 | `project_application` | `参加企業/管理会社/SaaS` — a header from column I leaked into the カラム名 column |
| 190 | `monitoring_calculation` | `is_standardize emission_act` — two column names in one cell |

Row 190 is why `MonitoringCalculationRequest` has both `isStandardizeEmissionAct`
and a separate `emissionAct`: the generator split the cell, and it happened to
land on something workable. It could equally have produced one nonsense field.

### 7.4 `other_project_operator` has no シート名

The only group of the 34 with the column blank. Its sheet is
代表以外のプログラム型運営・管理者.

### 7.5 Misspellings carried into the code

The spec's table names are the source of the DTO and slug names, so its typos
are now in the API surface and in DynamoDB sort keys:

| Spec | Should be |
|---|---|
| `equipment_informatio_verification_method` | `..._information_...` |
| `additional_assesment_method` | `..._assessment_...` |
| `obtaining_additional_assesment` | `..._assessment` |
| `post_impliment_incedental_activity` | `post_implement_incidental_activity` |
| `incitdental_emission_activity` | `incidental_...` |
| `monitoring_method_explaination` | `..._explanation` |
| `coefficient_measure_explaination` | `..._explanation` |
| `buisiness_name`, `other_contact_buisness_name` | `business_...` |
| `project_location_classificaton_en` | `..._classification_en` |

Renaming these is cosmetic but not free: the slugs are URLs and the sort keys
are stored, so it is a migration, not a rename.

### 7.6 What the spec got right

Worth saying, because it is most of it: the per-field 文字種チェック / 記号使用制限 /
長さ / NULL不可 columns are detailed and were followed faithfully. The bugs that
came out of them — 全角 rules rejecting ASCII, half-width rules rejecting
t-CO2/年 — came from applying them literally, not from the spec omitting them.
