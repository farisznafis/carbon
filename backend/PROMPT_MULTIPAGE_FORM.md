# Prompt: building a multi-page form from an Excel specification

For any feature that is **one long form split across many pages**, specified by
an Excel workbook, filled in over several sittings. Not tied to one feature —
replace the placeholders in §4.

Three messages. Do not ask for a DTO in the first one: the assistant has to
understand the framework before it sees a page, or it will write a fresh
controller for every page and the framework never takes shape.

| | When | What |
|---|---|---|
| Message 1 | once | the briefing, and the framework |
| Message 2 | per page | one Excel sheet |
| Message 3 | as needed | corrections when it drifts |

---

## 1. First message — brief it, then get the framework

Copy the block below as-is, after filling in the `{...}` parts.

````
You are helping me build the backend of a multi-page form.
You have no access to my repository — everything you need is in this message.

## Context

Feature: {FEATURE}
The form has roughly {COUNT} pages. People fill it in over several sittings, so
each page must save on its own and be resumable later. Each page's
specification comes from an official Excel workbook, which I will send one
sheet at a time once the framework exists.

Stack: Java 17, Spring Boot 3, Spring Validation (Jakarta), Lombok,
{STORAGE}, and PostgreSQL for master data.
Base package: {PACKAGE}

## The architectural rule that cannot be broken

ONE controller, ONE service, ONE repository — for ALL pages.
Adding a page is one DTO class plus one registry line. Never a new endpoint,
service or repository per page.

If a request seems to need one, STOP and explain why. Do not build it quietly.

## Invariants

1. PAGE ATOMICITY. A page is stored only if it validates in full. Nothing is
   half-saved. This is what makes the progress data trustworthy, which is what
   makes "resume" land on the right page.
2. THE SERVER OWNS DERIVED VALUES. Anything computed — totals, row skeletons,
   day counts — is recomputed on every read and every save, and OVERWRITES
   whatever the client sent. That is what makes it safe to send a total to the
   client at all.
3. DERIVATIONS MUST BE IDEMPOTENT. Saving applies them twice.
4. KEYS AND AUDIT FIELDS ARE SERVER-MANAGED. id, status, updatedAt, updatedBy
   are read-only on deserialise, so a client cannot forge a "complete" status.
5. THE DATA FLOWS ONE WAY, AND THIS IS ABSOLUTE.
   PostgreSQL is READ-ONLY here. It does two jobs: deciding who may access
   what, and supplying master data for autofill. NOTHING from this form is
   ever written back to PostgreSQL — not even on the final submit.
   ALL form data is stored in {STORAGE}, one item per page. "Complete" is an
   attribute on that item, NOT a column in PostgreSQL.
   Consequence: the save method stays annotated @Transactional(readOnly = true),
   because the only SQL work in it is the authorization read. If anyone ever
   adds a write to PostgreSQL, that annotation MUST come off — and that is the
   signal that this rule is being broken.

## What I want now — the framework only, no pages yet

1. An abstract base class for every page, holding the keys and the metadata
   (status, updatedAt, updatedBy, attachments). Use @Getter/@Setter, NOT
   @Data, so that @Data subclasses do not inherit a conflicting
   equals/hashCode contract.

2. An empty marker interface for a row of a repeating table. This is how the
   framework finds a page's tables without relying on a naming convention.

3. A FormDefinition record — everything needed to serve one page:
     slug            URL segment
     storageKey      storage key
     displayName     the label a user sees
     order           position; contiguous, no gaps
     type            that page's DTO class
     sqlFields       fields filled automatically from PostgreSQL
     derivation      values the server computes; null for most pages
     optional        a page only some people file

4. A registry: one component listing every page, built in its constructor.
   Fail at startup on a duplicate slug or key, or a gap in the ordering.
   Provide lookup by slug and by storage key.

5. A generic repository for all pages. Cache one table per DTO class. Provide
   find(type, key), save(type, item), and ONE query that reads the status of
   every page at once — not N reads.

6. A service with exactly this order of operations:

   READ:
     resolve the page from the registry -> authorize -> read the stored item
     (or a fresh instance) -> fill from SQL -> run the derivation -> return the
     data, the list of read-only fields, and the conditional rules.

   SAVE:
     bind the body -> RUN THE DERIVATION -> validate -> on any violation,
     return 400 and STORE NOTHING -> promote uploaded files -> re-read the
     payload and RUN THE DERIVATION AGAIN -> set keys/status/audit fields
     server-side -> save.

   The derivation runs before validation so that whatever the client sent for
   a derived field never reaches validation or storage. It runs again after
   file promotion because the re-read comes from the request body, which never
   carried the derived values.

   PROGRESS:
     every page with its status. The total and completed counts include
     REQUIRED pages only. An optional page is still listed, but never counted
     and never the target of "resume".

7. A @RequiredIf annotation: class-level, repeatable, with field(), when() and
   is(). A form like this is full of "pick yes/no; if yes, also fill these".

   IMPORTANT: the validator must report the violation against the DEPENDENT
   FIELD's node — disableDefaultConstraintViolation(), then
   buildConstraintViolationWithTemplate(...).addPropertyNode(field). A per-DTO
   @AssertTrue helper keys the error on the METHOD name instead, which breaks
   the field-keyed error contract the client depends on.

   Throw if the annotation names a property that does not exist. A typo there
   must not pass validation silently.

8. A controller: five endpoints for all pages — read one page, save one page,
   upload a file, progress for one parent, progress for many parents. Bind the
   body as JsonNode, then treeToValue into the DTO type. Do not use
   convertValue — it throws IllegalArgumentException, which usually surfaces
   as HTTP 500.

## Two traps you will hit

A. Filling from SQL must NOT simply overwrite. Run the binders against a
   throwaway instance first, serialise it, and report as read-only ONLY the
   fields that came back non-null — then apply just those to the real object.
   Skip this and a field SQL has no value for arrives EMPTY AND LOCKED, and a
   required field in that state makes the page permanently unsaveable.

B. Generics. The registry lookup returns FormDefinition<?>, which will not
   typecheck against repository.find(def.type(), ...). Every generic boundary
   in the service needs a private capture helper:
   private <T extends FormBase> X read(FormDefinition<T> def, ...)

## Validation

Use the standard Jakarta annotations only: @NotBlank, @NotNull, @Size, @Email,
@DecimalMin, @DecimalMax, @PositiveOrZero, and @Pattern with an inline regex.
Do NOT write your own character-set validator annotations. @RequiredIf above
is the only custom one.

## Done when

- A page can be registered with one DTO and one registry line, and is served
  by the shared controller with no other change.
- An invalid PUT returns 400, and a GET after it still shows that page as NOT
  STARTED. Nothing was half-stored.
- A valid PUT returns 200; the GET after it returns the saved values with the
  SQL-sourced fields overlaid and a non-empty read-only list.
- Saving page B leaves page A intact.
- The progress endpoint reports each page's status and points at the first
  unfinished REQUIRED page.
- A page holding a List<Row> round-trips through storage and reports a row
  error as rows[1].fieldName.
- A derived value is recomputed on save even when the client posts a different
  one.

Write those as tests. When you are done, ask me for the first sheet.
Do not write any DTO before I send you a sheet.
````

---

## 2. Second message onwards — send one sheet

One sheet per message. Two at a time and the answers go shallow.

````
Here is the next sheet.

Sheet name: {SHEET_NAME}
Rows {FROM}–{TO}

Headers and column labels:
{paste here}

Notes (※) on the sheet:
{paste here, do not summarise}

A filled example from the official sample file:
{paste 1–2 real rows, DO NOT blank them out}

Return exactly two things, with no long explanation:

A. The DTO class. Rules:
   - Each field's label from the sheet becomes its javadoc, verbatim.
   - @JsonProperty on every field.
   - NO @Builder or @AllArgsConstructor — Lombok's builder omits superclass
     fields, so the keys become unsettable WITH NO COMPILE ERROR.
   - If the sheet has a yes/no toggle governing other fields, add a
     class-level @RequiredIf.

B. One registry line for this page.

If this sheet contains a REPEATING TABLE:
   One row of the sheet is one row object. Do not flatten a table into a
   single set of columns, and do not split one table's columns across two
   pages.
   Write a row class implementing the marker interface, and hold it on the
   page as a List<Row> with @Valid and @NotEmpty.
   Where two tables share most of their columns, put the shared ones on an
   abstract base; only the concrete row classes are annotated as storage
   beans.

If the sheet says a column is computed, that a total is always in a particular
unit, or that rows may be added — SAY SO in your answer and propose a
server-side derivation rather than making the person type it.

Validation rules — this is where mistakes are most common, read before writing
any @Pattern or @Size:

1. A field the official example LEAVES BLANK must not be @NotBlank.
   A database specification will mark almost everything required. The form
   does not. Because a page is all-or-nothing, one wrongly-required field
   makes the whole page unsaveable.

2. Take @Size from the example, do not guess. Measure the longest value and
   leave headroom.

3. These get no character-set rule at all: units, symbols, formulas, account
   numbers, codes, classification letters. They mix scripts by nature, and any
   one-sided rule rejects half of what the form actually contains.

4. Before adding a @Pattern, find the value in the example and check the
   pattern accepts it.
````

---

## 3. When it drifts

The three corrections most often needed. Copy whichever applies.

````
You wrote a new controller/service/repository for this page. Don't. The
architecture is one controller for all pages. Return only the DTO and one
registry line.
````

````
You marked this field required, but the official example leaves it blank.
Because this page is atomic, that makes the whole page unsaveable. Make it
optional.
````

````
That @Pattern rejects a value the official example contains: {VALUE}. Check
the pattern against the example again, or drop the character-set rule if this
field genuinely mixes scripts.
````

---

## 4. Placeholders to fill in

| Placeholder | Example |
|---|---|
| `{FEATURE}` | プロジェクト計画書 (J-Credit project plan) |
| `{COUNT}` | 32 |
| `{STORAGE}` | AWS SDK v2 DynamoDB Enhanced Client |
| `{PACKAGE}` | `com.kct.jcredit.core_backend` |
| `{SHEET_NAME}` | `【HP公開】4.排出削減計画` |

If storage is relational tables per page rather than documents, replace point 5
in the first message — nothing else changes, because the registry, the save
ordering and the validation rules do not depend on the storage engine.

---

## 5. Why these rules exist

This section is for you, not for pasting. All of it came from real defects.

**Character-set rules reject real data.** Five separate cases: the unit
`t-CO2/年` rejected by a half-width rule; the classification `Ⅰ Ⅱ Ⅲ` rejected by
a Japanese-text rule because Roman numerals fall outside the pattern; formulas
rejected because `=` and `×` were not in the pattern; personal names rejected
over the space between given and family name; and one region rejected even
though it is the only official value for that region.

**The blank format is the form; the example is one way to fill it.** Reading
structure out of the sample once led me to add a table nobody had asked for.
But the example still has to be sent — it is the only way to know column
lengths and which columns may be left blank.

**Derived fields once never appeared on screen at all.** The server computed
them on save and returned them, but the client discarded the response, so the
numbers only caught up when the user navigated away and back. If a frontend is
being built too, make sure the save response is folded back into the form.
