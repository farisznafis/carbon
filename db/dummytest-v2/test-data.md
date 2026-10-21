# Dummy Test V2 data catalogue

> All identities and documents are fictional test data. Baseline: **2026-10-09**. Seed activity start: **2026-08-01**. Seed monitoring: **2026-09-15** and **2026-09-30**.

## Accounts

Seed password for every listed login: `GxTest2026!Solar`. Manual-created master credentials come from the real setup flow, not from this seed password.

> **Email classification:** every `example.test` address below is **SEED ONLY**. Do not enter it in an AWS browser form or use it for direct SES delivery. If an action on a seeded account sends email, keep `APP_MAIL_OVERRIDE_TO=<SES_RECEIVER_01>` enabled. Browser-created accounts use unique addresses under `<TEST_EMAIL_DOMAIN>`; only the six `<SES_RECEIVER_0N>` slots are verified physical receivers.

| Flow | Stored account email | Physical delivery |
|---|---|---|
| Static integration seed | Existing unique `example.test` value | No email expected; use override if triggered |
| Normal manual E2E | Unique `name@<TEST_EMAIL_DOMAIN>` | `<SES_RECEIVER_01>` through override |
| Direct-delivery smoke test | One unused verified receiver | Same address; override disabled |
| DocuSign | Separately selected reachable signer | Not affected by SES override |

| Role | Username | Email | Scope |
|---|---|---|---|
| Admin | `it.admin` | `admin@example.test` | OP000000001 |
| Operator | `it.operator` | `operator@example.test` | OP000000001 |
| Master | `master.c01` | `master.c01@example.test` | C01 |
| Master | `master.c02` | `master.c02@example.test` | C02 |
| Master | `master.c03` | `master.c03@example.test` | C03 |
| Master | `master.c04` | `master.c04@example.test` | C04 |
| Master | `master.c05` | `master.c05@example.test` | C05 |
| Master | `master.c06` | `master.c06@example.test` | C06 |
| Site user | `user.s01` | `user.s01@example.test` | S01 |
| Site user | `user.s02` | `user.s02@example.test` | S02 |
| Site user | `user.s03` | `user.s03@example.test` | S03 |
| Site user | `user.s04` | `user.s04@example.test` | S04 |
| Site user | `user.s05` | `user.s05@example.test` | S05 |
| Site user | `user.s06` | `user.s06@example.test` | S06 |
| Site user | `user.s07` | `user.s07@example.test` | S07 |
| Site user | `user.s08` | `user.s08@example.test` | S08 |

## Projects

| Code | Number / public ID | Name | Methodology | Approved equipment |
|---|---|---|---|---:|
| P01 | `P000001` / `ITPROJ001` | 地域太陽光発電プログラム第一期 | EN-R-002 Ver.3.7 | 5 |
| P02 | `P000002` / `ITPROJ002` | 地域太陽光発電プログラム第二期 | EN-R-002 Ver.3.7 | 0 |

Both projects intentionally use method number `002`; project number/public ID remains unique.

## Companies and sites

| Company | Identity | Master | Sites |
|---|---|---|---|
| C01 | `PC000000001` 株式会社さくらエナジー / 9100000000001 | `master.c01` | S01 さくらエナジー本社太陽光発電所<br>S02 さくらエナジー物流センター太陽光発電所 |
| C02 | `PC000000002` 株式会社みなと物流 / 9100000000002 | `master.c02` | S03 みなと物流本社太陽光発電所<br>S04 みなと物流物流センター太陽光発電所 |
| C03 | `PC000000003` 株式会社あおば製作所 / 9100000000003 | `master.c03` | S05 あおば製作所本社太陽光発電所 |
| C04 | `PC000000004` 株式会社北海食品 / 9100000000004 | `master.c04` | S06 北海食品本社太陽光発電所 |
| C05 | `PC000000005` 株式会社東都印刷 / 9100000000005 | `master.c05` | S07 東都印刷本社太陽光発電所 |
| C06 | `PC000000006` 株式会社信州精密 / 9100000000006 | `master.c06` | S08 信州精密本社太陽光発電所 |

## Equipment and participation states

| Equipment | Company / site / project | Output / PCS / device | Battery | Subsidy | State |
|---|---|---|---|---|---|
| E01 | C01 / S01 / P01 | 35 kW / `TS-PCS-35` / `IT-S01-PCS-1` | TS-BAT-40 / 40 kWh | No | `APPROVED` |
| E02 | C01 / S02 / P01 | 40 kW / `TS-PCS-40` / `IT-S02-PCS-1` | TS-BAT-40 / 40 kWh | 1600000 JPY | `APPROVED` |
| E03 | C02 / S03 / P01 | 45 kW / `TS-PCS-45` / `IT-S03-PCS-1` | No | No | `APPROVED` |
| E04 | C02 / S04 / P01 | 50 kW / `TS-PCS-50` / `IT-S04-PCS-1` | TS-BAT-40 / 40 kWh | 2000000 JPY | `APPROVED` |
| E05 | C03 / S05 / P01 | 55 kW / `TS-PCS-55` / `IT-S05-PCS-1` | TS-BAT-40 / 40 kWh | No | `APPROVED` |
| E06 | C04 / S06 / P01 | 60 kW / `TS-PCS-60` / `IT-S06-PCS-1` | No | 2400000 JPY | `OPEN` |
| E07 | C05 / S07 / P01 | 65 kW / `TS-PCS-65` / `IT-S07-PCS-1` | TS-BAT-40 / 40 kWh | No | `OPEN` |
| E08 | C06 / S08 / P01 | 70 kW / `TS-PCS-70` / `IT-S08-PCS-1` | TS-BAT-40 / 40 kWh | 2800000 JPY | `REJECTED` |

- E01–E05: approved and have M1/M2 monitoring.
- E06: submitted, no review (`OPEN`).
- E07: primary review exists, waiting for secondary review (`OPEN`).
- E08: rejected; rejection reason is in `dynamodb/review-history.json`.

## Monitoring inputs

| Record | Equipment / site | Date | ELpv | ELpvr | Control | Battery |
|---|---|---|---:|---:|---:|---:|
| E01-M1 | E01 / S01 | 2026-09-15 | 1225 | 245.00 | 24.50 | 12.25 |
| E02-M1 | E02 / S02 | 2026-09-15 | 1400 | 280.00 | 28.00 | 14.00 |
| E03-M1 | E03 / S03 | 2026-09-15 | 1575 | 315.00 | 31.50 | — |
| E04-M1 | E04 / S04 | 2026-09-15 | 1750 | 350.00 | 35.00 | 17.50 |
| E05-M1 | E05 / S05 | 2026-09-15 | 1925 | 385.00 | 38.50 | 19.25 |
| E01-M2 | E01 / S01 | 2026-09-30 | 1120 | 224.00 | 22.40 | 11.20 |
| E02-M2 | E02 / S02 | 2026-09-30 | 1280 | 256.00 | 25.60 | 12.80 |
| E03-M2 | E03 / S03 | 2026-09-30 | 1440 | 288.00 | 28.80 | — |
| E04-M2 | E04 / S04 | 2026-09-30 | 1600 | 320.00 | 32.00 | 16.00 |
| E05-M2 | E05 / S05 | 2026-09-30 | 1760 | 352.00 | 35.20 | 17.60 |

CEF for the static calculation snapshot: `0.000512`. Expected per-equipment and project totals are stored beside this document in `expected-results.json`.

## File coverage

- Project files: two plan XLSX files plus registration/validation PDFs.
- Equipment files: contract, panel, PCS, invoice and electricity bill for all eight equipment; battery/subsidy documents only when applicable.
- Monitoring files: generation, reverse flow and control for ten records; battery evidence only for battery equipment.
- Membership PDFs: E01–E05.
- Complete filled Markdown sheet sets: P01/P02 project plan, P01 individual activity, P01 monitoring report.
