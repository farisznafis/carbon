# GX Carbon — Dummy/Seed Data Environment (Tables 1–17)
> Scope: 20 tables total. 
> Tables 18–20 (batch calculation) are documented separately 
> For overview please do ctrl + shift + v 


---
## 1. Quick facts

| | |
|---|---|
| Operator companies | 1 (Creation Tech Co., Ltd. / OP000000001) |
| Operating users (operator-side logins) | 4 — `operator1`–`operator4` |
| Participating companies | 4 — 2 ACTIVE, 1 SUSPENDED, 1 REJECTED |
| Participating users (company-side logins) | 10 — 4 master, 6 user |
| Sites | 10 |
| Bank accounts | 2 |
| Projects | 2 — 1 ACTIVE, 1 DRAFT |
| Total login accounts | 14 |
| Password (all accounts) | `password123`, bcrypt cost factor **10** (not 12 — flag this if app config assumes 12) |


---

## 2. Login accounts (for testers)

### Operating users — Creation Tech Co., Ltd.

| Username | Name | Email | Role | Notes |
|---|---|---|---|---|
| operator1 | Maya Masuda | operator1@creation-tech.co.jp | operator | Created by `system`; also the operator company's registered legal-rep contact (`rep_email` still shows `masu.maya@...` — that field wasn't renamed, see §6) |
| operator2 | Sana Torii | operator2@creation-tech.co.jp | operator | Standard operator |
| operator3 | Yuna Kato | operator3@creation-tech.co.jp | operator | Email OTP login enabled, `auth_version=1` |
| operator4 | Yumi Mori | operator4@creation-tech.co.jp | operator | Standard operator |

### Participating users — by company

**① Solar Farm Co., Ltd. (PC000000001 / ACTIVE)**

| Username | Name | Role | Site | Position |
|---|---|---|---|---|
| ito.kenichi | Kenichi Ito | master | Shibuya HQ | Representative Director |
| ito.akiko | Akiko Ito | user | Shinagawa Logistics Center | Facility Manager |
| ito.jun | Jun Ito | user | Tachikawa R&D Facility | Operations Staff |
| ito.misaki | Misaki Ito | user | Hachioji Plant | General Affairs |

**② Eco Life Ltd. (PC000000002 / ACTIVE — past suspension, resolved)**

| Username | Name | Role | Site | Position |
|---|---|---|---|---|
| nakamura.sachiko | Sachiko Nakamura | master | Yokohama Office | Rep. Director |
| nakamura.ryota | Ryota Nakamura | user | Kawasaki Warehouse | Facility Staff |
| nakamura.yui | Yui Nakamura | user | Yokohama Office (site_id=5) | Admin Staff |

**③ Kuma Power Co., Ltd. (PC000000003 / SUSPENDED — currently unresolved)**

| Username | Name | Role | Site | Position |
|---|---|---|---|---|
| kumagai.takuya | Takuya Kumagai | master | Nagoya Plant | Rep. Director — login may be affected by the company suspension, check your app's auth rules |
| kumagai.nao | Nao Kumagai | user | Toyota Logistics Facility | Facility Manager |

**④ Asa Denki Co., Ltd. (PC000000004 / REJECTED)**

No participating_user records exist for this company — nothing to log in as here. Rei Asano appears only as each site's PIC (point of contact) field, not as an actual user account.

---

## 3. Participating company status — why each one is what it is

| Company | Status | is_active | Why |
|---|---|---|---|
| Solar Farm (PC1) | ACTIVE | TRUE | Clean, fully onboarded, running Project 1 |
| Eco Life (PC2) | ACTIVE | TRUE | Was suspended once (`company_suspension` row 2, POLICY_VIOLATION, missing documents) — resolved 2026-07-18, back to normal. Not currently linked to any project. |
| Kuma Power (PC3) | **SUSPENDED** | FALSE | Unresolved `company_suspension` row (ADMINISTRATIVE, `resolved_at` still NULL) plus a notification already narrating them as suspended. Still owns Project 2 (DRAFT) — a suspended company with a stalled draft project is a realistic, useful test case. |
| Asa Denki (PC4) | **REJECTED** | FALSE | A `REJECTED_SIGNUP_REVISION` security_action_link exists for them (created 2026-07-22, still unopened) — only makes sense after an actual rejection, so `rejected_at`/`last_rejection_reason` were populated and status set to REJECTED. |

**Note:** this leaves no company in a plain `PENDING` (never-reviewed) or `APPROVED_PENDING_SETUP` state. If you need those states covered too, easiest is a 5th participating_company row rather than repurposing one of these four.

---

## 4. Sites (10 total)

| Company | Sites | Site IDs |
|---|---|---|
| Solar Farm | 4 | 1–4 |
| Eco Life | 2 | 5–6 |
| Kuma Power | 2 | 7–8 |
| Asa Denki | 2 | 9–10 |

**Linked to a project:** 6/10 (60%) — sites 1–4 (Project 1, ACTIVE) and sites 7–8 (Project 2, DRAFT).
**Not linked to any project:** 4/10 — sites 5–6 (Eco Life) and 9–10 (Asa Denki). This is intentional: those two companies haven't joined a project pipeline yet, which is realistic for ACTIVE/REJECTED companies that exist but aren't (yet) part of a collective project.

---

## 5. Projects

| Project | Status | Sites | Equipment | Members |
|---|---|---|---|---|
| P000001 (Solar Farm, EN-R-002) | ACTIVE | 1–4 | 1–4 | 4 participating_project records (users 1–4, each tied 1:1 to their site's equipment) |
| P000002 (Kuma Power, EN-R-003†) | DRAFT | 7–8 | 5–6 | 0 — crediting period starts 2026-08-01, hasn't begun |

† **Flag, not yet resolved:** Project 2's equipment lives in `en_r002_equipment` — a table that's architecturally specific to methodology EN-R-002 (per your own membership-registration redesign notes), yet Project 2 is registered under method_number `'003'`. `project.method_number` is UNIQUE, so both projects can't hold `'002'` at once under the current schema. Worth deciding whether that uniqueness constraint is really meant to be global, or whether Project 2 needs its own equipment table eventually.

---

## 6. Known deliberate quirks (not bugs — just worth knowing)

- `operator_company.rep_email` still reads `masu.maya@creation-tech.co.jp` — that's the company's own legal-representative contact field (a business/registration detail), not a login field, so it wasn't renamed along with the operating_user accounts.
- Passwords: bcrypt hash reused across every test account, cost factor **10**. If your Spring Boot `BCryptPasswordEncoder` default is configured for cost 12, that's fine for verifying logins (bcrypt reads cost from the hash itself) — just don't expect newly-registered-in-app passwords to match this hash's cost if you ever compare them directly.
- `participating_company.requested_methodologies`: 3 of 4 rows are clean JSON arrays; PC000000004 needs fixing to `["EN-R002","EN-S003"]` (see top of this response).

---

## 7. Table reference (1–17)

| # | Table | Purpose |
|---|---|---|
| 1 | operator_company | Platform operator company info |
| 2 | participating_company | Participating (power-generating) company info & approval status |
| 3 | site | Power plant / facility sites owned by participating companies |
| 4 | operating_user | Login accounts for operator-side staff |
| 5 | participating_user | Login accounts for company-side staff (master/user roles) |
| 6 | account | Bank account info for payments/transfers |
| 7 | project | Carbon credit generation project entity |
| 8 | site_project_relation | Links sites to projects (activity start date, status) |
| 9 | en_r002_equipment | Solar power equipment details (EN-R-002 methodology) |
| 10 | en_r002_equipment_file | Equipment-related files (specs, install photos) |
| 11 | participating_project | User-level project participation & membership certificates |
| 12 | project_files | Project-level certificates & validation/verification reports |
| 13 | company_suspension | Suspension history (resolved/unresolved) per company |
| 14 | company_deletion_request | Company withdrawal/deletion requests & review outcome |
| 15 | record_cancel_request | Monitoring-record cancellation requests (offset info only once approved) |
| 16 | security_action_link | Security-related action links (password reset, master setup, rejection-revision) |
| 17 | notification | System notifications (suspensions, rejections, cancellations, etc.) |
| 18–20 | batch_process_history / en_r002_reduction_result / en_r002_reduction_result_total | See separate batch-calculation deliverable |