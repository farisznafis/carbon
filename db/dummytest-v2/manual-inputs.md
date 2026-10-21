# Manual frontend input cards

Use these cards only with the manual flow in [manual-test.md](manual-test.md). Copy the value inside backticks. Values marked **Actual** must come from records created during the current browser run.

> Addresses ending in `example.test` are seed-only. For browser entry, replace `<TEST_EMAIL_DOMAIN>` below with a domain that has an MX or A record. Keep every logical account email unique. With `APP_MAIL_OVERRIDE_TO=<SES_RECEIVER_01>`, logical emails do not need individual SES verification.

## Email legend

| Marker | Meaning |
|---|---|
| `<TEST_EMAIL_DOMAIN>` | DNS-resolvable domain used for unique stored account identities |
| `<SES_RECEIVER_01>`…`<SES_RECEIVER_06>` | Actual addresses verified in SES sandbox; never commit their values |
| `APP_MAIL_OVERRIDE_TO` | Physical delivery target for the normal E2E flow |

Use a verified receiver as a stored account email only for the explicit direct-delivery smoke test, with the override disabled.

## Shared values

| Item | Value |
|---|---|
| Manual password | `GxTest2026!Solar` |
| Methodology | `EN-R-002` / `太陽光発電設備の導入` / `Ver.3.7` |
| Data manager | `鳥居 沙奈` |
| Operator company code | `OP000000001` |
| Region | `全国` |
| Initial baseline emission | `0` |
| Initial post-project emission | `0` |

## 1. Operator account

| Visible field | Paste/select |
|---|---|
| 姓 | `鳥居` |
| 名 | `沙奈` |
| 姓（カナ） | `トリイ` |
| 名（カナ） | `サナ` |
| メールアドレス | `operator@<TEST_EMAIL_DOMAIN>` |
| 電話番号 | `0355550102` |
| ユーザー名 | `it_operator` |
| ロール | Operator |
| 運営会社ID | `OP000000001` |

Assign project create/read/update/status/output, participating-company read/review, monitoring read/delete/cancel/history/error, operator read, methodology read and application-form read permissions.

## 2. Projects

| Visible field | P01 | P02 |
|---|---|---|
| プロジェクト名 | `地域太陽光発電プログラム第一期` | `地域太陽光発電プログラム第二期` |
| プロジェクト番号 | `P000001` | `P000002` |
| ステータス | `ACTIVE` | `ACTIVE` |
| 地域区分 | `全国` | `全国` |
| 用途 | `地域事業所の排出削減とクレジット創出。` | `地域事業所の排出削減とクレジット創出。` |
| 方法論区分 | `EN-R` | `EN-R` |
| 方法論番号 | `002` | `002` |
| 方法論版 | `Ver.3.7` | `Ver.3.7` |
| 方法論名 | `太陽光発電設備の導入` | `太陽光発電設備の導入` |
| 主な排出活動 | `電力使用に伴う二酸化炭素排出量` | Same |
| 組合せ可否 | Yes | Yes |
| 詳細 | `自家消費型太陽光発電設備による排出削減活動。` | Same |
| 代表者 | `増田 真耶` / `マスダ マヤ` | Same |
| 代表者メール | `admin@<TEST_EMAIL_DOMAIN>` | Same |
| 代表者電話 | `0355550101` | Same |
| 代表者役職 | `代表取締役` | Same |
| 担当者 | `鳥居 沙奈` / `トリイ サナ` | Same |
| 担当者メール | `operator@<TEST_EMAIL_DOMAIN>` | Same |
| 担当者電話 | `0355550102` | Same |
| 担当者役職 | `プロジェクト管理担当` | Same |
| データ管理責任者 | `鳥居 沙奈` | Same |
| 基準排出量 | `0` | `0` |
| プロジェクト後排出量 | `0` | `0` |
| Project plan | `fixtures/project-plan/P01-plan.xlsx` | `fixtures/project-plan/P02-plan.xlsx` |

Opening member counts are `0`. Counts produced later by approvals are results to inspect, not values to force during project creation.

## 3. Company signup — C01 golden path

| Visible field | Paste/select |
|---|---|
| 法人番号 | `9100000000001` |
| 会社名 | `株式会社さくらエナジー` |
| 会社名カナ | `サクラエナジー` |
| English name | `Sakura Energy Co., Ltd.` |
| 会社電話番号 | `0355560001` |
| 郵便番号 | `1000005` |
| 都道府県 | `東京都` |
| 市区町村 | `千代田区丸の内` |
| 番地 | `１丁目２番３号` |
| 建物名 | `本社ビル` |
| 代表者 姓/名 | `佐藤` / `陽斗` |
| 代表者 姓/名カナ | `サトウ` / `ハルト` |
| 役職 | `代表取締役` |
| 代表者電話 | `0355560001` |
| Email | `master.c01@<TEST_EMAIL_DOMAIN>` |
| 希望方法論 | `EN-R002` |
| 活動内容 | `事業所の屋根に自家消費型太陽光発電設備を導入し、電力使用による排出量を削減します。` |

Approve the company as admin, open the delivered master-setup link, then set username `master_c01` and password `GxTest2026!Solar`.

## 4. Sites and users

| Field | S01 | S02 |
|---|---|---|
| Site name | `さくらエナジー本社太陽光発電所` | `さくらエナジー物流センター太陽光発電所` |
| Site kana | `サクラエナジーホンシャタイヨウコウハツデンショ` | `サクラエナジーブツリュウセンタータイヨウコウハツデンショ` |
| Postal code | `1000005` | `1000005` |
| Prefecture | `東京都` | `東京都` |
| Municipality | `千代田区丸の内` | `千代田区丸の内` |
| Block | `1丁目２番３号` | `2丁目２番３号` |
| Building | `本社棟` | `物流センター棟` |
| User surname/given | `渡辺` / `結衣` | `山本` / `拓也` |
| Kana | `ワタナベ` / `ユイ` | `ヤマモト` / `タクヤ` |
| Position | `設備管理担当` | `設備管理担当` |
| Phone | `0355561001` | `0355561002` |
| Username | `user_s01` | `user_s02` |
| Email | `user.s01@<TEST_EMAIL_DOMAIN>` | `user.s02@<TEST_EMAIL_DOMAIN>` |
| Password | `GxTest2026!Solar` | `GxTest2026!Solar` |

After user registration, edit the site and select the actual user as site PIC.

## 5. Equipment

### E01 — battery, no subsidy

| Visible field/control | Paste/select |
|---|---|
| Site | **Actual S01** |
| Change date | Leave blank |
| Operation date | Use the actual project-join/activity date |
| Nominal maximum output | `35` |
| PCS manufacturer | `Test Solar Systems` |
| PCS model | `TS-PCS-35` |
| Device number | `IT-S01-PCS-1` |
| PCS standby power | `0.30` |
| Battery | Yes |
| Battery maker | `Test Storage` |
| Battery model | `TS-BAT-40` |
| Battery capacity | `40` |
| Total investment | `7700000` |
| Subsidy | No |
| Pre-project unit price | `28.50` |
| Contract | `fixtures/equipment/E01/solar_contract.pdf` |
| Panel specification | `fixtures/equipment/E01/panel_catalog.pdf` |
| PCS specification | `fixtures/equipment/E01/pcs_spec.pdf` |
| Battery specification | `fixtures/equipment/E01/battery_catalog.pdf` |
| Invoice | `fixtures/equipment/E01/construction_invoice.pdf` |
| Electricity bill | `fixtures/equipment/E01/electricity_bill.pdf` |

### E02 — battery and subsidy variation

Use **Actual S02**, output `40`, PCS `TS-PCS-40`, device `IT-S02-PCS-1`, investment `8800000`, battery `TS-BAT-40 / 40`, subsidy `地域再生可能エネルギー導入補助金 / 1600000`, and the files under `fixtures/equipment/E02/`.

## 6. Agreements and reviews

| Screen | Input |
|---|---|
| Data-use confirmation | Data use, additional data, transfer and not registered elsewhere = Yes |
| Business declarations | Keidanren participation, emitter and periodic reporting = No |
| Sustainability | Company `株式会社さくらエナジー`; representative `佐藤陽斗`; role `代表取締役`; consent = Yes |
| Terms | Agree = Yes |
| Primary review | Admin `増田 真耶`; confirm every applicable checklist; seal `増田` |
| Secondary review | Different account `鳥居 沙奈`; seal `鳥居` |
| Rejection variation | `運転開始日の証憑が不鮮明です。再提出してください。` |

## 7. Monitoring

For a participation created through the UI, use a measurement date between the **actual activity start date** and today. Do not copy the seeded dates if they are outside that range.

| Field | E01-M1 | E01-M2 |
|---|---:|---:|
| Equipment | **Actual E01** | **Actual E01** |
| Measurement date | Actual join date | A later valid date |
| ELpv | `1225` | `1120` |
| ELpvr | `245` | `224` |
| ELPJ control | `24.5` | `22.4` |
| ELPJ battery | `12.25` | `11.2` |
| Accuracy | `99.0` | `99.0` |
| Generation evidence | `fixtures/monitoring/E01-1-generation.pdf` | `fixtures/monitoring/E01-2-generation.pdf` |
| Reverse-flow evidence | `fixtures/monitoring/E01-1-reverse-flow.pdf` | `fixtures/monitoring/E01-2-reverse-flow.pdf` |
| Control evidence | `fixtures/monitoring/E01-1-control.pdf` | `fixtures/monitoring/E01-2-control.pdf` |
| Battery evidence | `fixtures/monitoring/E01-1-battery.pdf` | `fixtures/monitoring/E01-2-battery.pdf` |

Seeded integration records use the fixed dates and values listed in [test-data.md](test-data.md).
