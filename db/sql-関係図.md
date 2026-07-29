SQLの関係図

> 2026-07-28 時点の `db/GXCarbonSQL_current_2026-07-28.sql`（全 `@Entity` に完全準拠したベースライン）に合わせて更新。

```mermaid
erDiagram
    %% <master / operator entities>
    operator_company {
        int id PK "auto-increment"
        boolean is_active "(nn)"
        timestamp created_at
        timestamp updated_at
        varchar created_by
        varchar updated_by
        varchar company_id UK "(nn) KCT operator ID"
        varchar name_ja "(nn)"
        varchar name_furigana "(nn)"
        varchar name_en "(nn)"
        varchar email "(nn)"
        varchar phone_number "(nn)"
        varchar corp_number UK "(nn) 13 digits"
        varchar postal_code "(nn) 000-0000"
        varchar prefecture "(nn)"
        varchar municipality "(nn)"
        varchar blocknum "(nn)"
        varchar building_name "nullable"
        varchar rep_first_name "(nn)"
        varchar rep_first_name_kana "(nn)"
        varchar rep_last_name "(nn)"
        varchar rep_last_name_kana "(nn)"
        varchar rep_email "(nn)"
        varchar rep_phone_number "(nn)"
        varchar rep_position "(nn)"
    }

    operating_user {
        int id PK "auto-increment"
        boolean is_active "(nn)"
        timestamp created_at
        timestamp updated_at
        varchar created_by
        varchar updated_by
        varchar username UK "(nn) case-insensitive unique index"
        varchar first_name "(nn)"
        varchar first_name_kana "(nn)"
        varchar last_name "(nn)"
        varchar last_name_kana "(nn)"
        varchar email "(nn) case-insensitive unique index"
        varchar phone_number "(nn)"
        varchar password "(nn) bcrypt hash"
        varchar role "(nn) operator, user"
        int login_attempts "(nn)"
        timestamp account_locked_until
        int company_id FK "(nn) references operator_company.id"
        boolean email_otp_login_enabled "(nn) default false"
        int auth_version "(nn) default 0"
    }

    account {
        int id PK "auto-increment"
        boolean is_active "(nn)"
        timestamp created_at
        timestamp updated_at
        varchar created_by
        varchar updated_by
        varchar acct_no UK "(nn) J-Credit account number"
        varchar holder_account_name_kana "(nn)"
        varchar holder_account_name "(nn)"
        varchar holder_corp_number UK "13 digits"
        varchar holder_prefecture "(nn)"
        varchar holder_city "(nn)"
        varchar holder_address "(nn)"
        boolean is_holder_undecided "(nn)"
        int operator_company_id FK "(nn)"
    }

    project {
        int id PK "auto-increment"
        boolean is_active "(nn)"
        timestamp created_at
        timestamp updated_at
        varchar created_by
        varchar updated_by
        varchar project_name "(nn)"
        varchar project_number UK "(nn) P[digits]"
        varchar project_status "(nn)"
        varchar rep_first_name "(nn)"
        varchar rep_first_name_kana "(nn)"
        varchar rep_last_name "(nn)"
        varchar rep_last_name_kana "(nn)"
        varchar rep_email "(nn)"
        varchar rep_phone_number "(nn)"
        varchar rep_position "(nn)"
        varchar pic_first_name "(nn)"
        varchar pic_first_name_kana "(nn)"
        varchar pic_last_name "(nn)"
        varchar pic_last_name_kana "(nn)"
        varchar pic_email "(nn)"
        varchar pic_phone_number "(nn)"
        varchar pic_position "(nn)"
        varchar data_management_pic "(nn)"
        decimal emission_reduction
        decimal baseline_emission
        decimal post_project_emission
        varchar regional_category "(nn) 北海道,東北,関東,中部,近畿,中国,四国,九州・沖縄,全国"
        int new_members_total "(nn)"
        int withdrawn_members "(nn)"
        int cumulative_members_total "(nn)"
        int members_total "nullable at DB level"
        varchar methodology_version "(nn) Ver.X"
        varchar method_category "(nn) EN-S,EN-R,IN,AG,WA"
        varchar method_number UK "(nn) 3 digits"
        varchar method_name "(nn)"
        varchar method_main_emission "(nn)"
        boolean method_combi_feasibility "(nn)"
        varchar method_details "(nn)"
        int operator_company_id FK "(nn)"
        varchar public_id UK "(nn) 32 chars, public-facing identifier"
    }

    %% <participating company & its users/sites>
    participating_company {
        int id PK "auto-increment"
        boolean is_active "(nn) default true; must equal (company_status = 'ACTIVE')"
        timestamp created_at
        timestamp updated_at
        varchar created_by
        varchar updated_by
        varchar company_id UK "(nn)"
        varchar name_ja "(nn)"
        varchar name_furigana "(nn)"
        varchar name_en "(nn)"
        varchar email "(nn)"
        varchar phone_number "(nn)"
        varchar corp_number UK "(nn) 13 digits"
        varchar postal_code "(nn) 000-0000"
        varchar prefecture "(nn)"
        varchar municipality "(nn)"
        varchar blocknum "(nn)"
        varchar building_name "nullable"
        varchar fax
        varchar rep_first_name "(nn)"
        varchar rep_first_name_kana "(nn)"
        varchar rep_last_name "(nn)"
        varchar rep_last_name_kana "(nn)"
        varchar rep_phone_number "(nn)"
        varchar rep_position "(nn)"
        text requested_methodologies
        text activity_information
        timestamp deleted_at "soft-delete"
        varchar deleted_by "soft-delete"
        varchar company_status "(nn) default PENDING; PENDING,APPROVED_PENDING_SETUP,ACTIVE,SUSPENDED,REJECTED"
        timestamp email_verified_at
        varchar last_rejection_reason
        timestamp rejected_at
    }

    site {
        int id PK "auto-increment"
        boolean is_active "(nn)"
        timestamp created_at
        timestamp updated_at
        varchar created_by
        varchar updated_by
        varchar site_name "(nn)"
        varchar postal_code "(nn) 000-0000"
        varchar prefecture "(nn)"
        varchar municipality "(nn)"
        varchar blocknum "(nn)"
        varchar building_name "nullable"
        varchar pic_first_name "(nn)"
        varchar pic_first_name_kana "(nn)"
        varchar pic_last_name "(nn)"
        varchar pic_last_name_kana "(nn)"
        varchar pic_email "(nn)"
        varchar pic_phone_number "(nn)"
        varchar pic_position "(nn)"
        int participating_company_id FK "(nn) ON DELETE CASCADE"
        timestamp deleted_at "soft-delete"
        varchar deleted_by "soft-delete"
        varchar public_id UK "(nn) 32 chars, public-facing identifier"
    }

    site_project_relation {
        int id PK "auto-increment"
        boolean is_active "(nn)"
        timestamp created_at
        timestamp updated_at
        varchar created_by
        varchar updated_by
        int project_id FK "(nn) UK with site_id, ON DELETE CASCADE"
        int site_id FK "(nn) UK with project_id, ON DELETE CASCADE"
        date activity_start_date "(nn)"
        varchar status "(nn)"
        timestamp deleted_at "soft-delete"
        varchar deleted_by "soft-delete"
    }

    %% <users>
    participating_user {
        int id PK "auto-increment"
        boolean is_active "(nn)"
        timestamp created_at
        timestamp updated_at
        varchar created_by
        varchar updated_by
        varchar username UK "(nn) case-insensitive unique index (live rows only)"
        varchar first_name "(nn)"
        varchar first_name_kana "(nn)"
        varchar last_name "(nn)"
        varchar last_name_kana "(nn)"
        varchar email "(nn) case-insensitive unique index (live rows only)"
        varchar phone_number "(nn)"
        varchar user_position "(nn)"
        varchar password "(nn) bcrypt hash"
        varchar role "(nn) master, user"
        int login_attempts "(nn)"
        timestamp account_locked_until
        int site_id FK "nullable, ON DELETE SET NULL"
        int company_id FK "(nn) references participating_company.id"
        timestamp deleted_at "soft-delete"
        varchar deleted_by "soft-delete"
        boolean email_otp_login_enabled "(nn) default false"
        int auth_version "(nn) default 0"
    }

    %% <project files>
    project_files {
        int id PK "auto-increment"
        boolean is_active "(nn) default true"
        timestamp created_at
        timestamp updated_at
        varchar created_by
        varchar updated_by
        int project_id FK "(nn) UNIQUE, ON DELETE CASCADE"
        text certificate_registration
        text report_validation
        text report_verification
    }

    %% <solar equipment: participating_project flow (current)>
    en_r002_equipment {
        int id PK "auto-increment"
        boolean is_active "(nn) default true"
        timestamp created_at
        timestamp updated_at
        varchar created_by
        varchar updated_by
        timestamp deleted_at "soft-delete"
        varchar deleted_by "soft-delete"
        int site_id FK "(nn) ON DELETE CASCADE"
        date equipment_op_date "(nn)"
        decimal nominal_max_output "(nn)"
        varchar pcs_manufacturer "(nn)"
        varchar pcs_model "(nn)"
        int pcs_unit_count
        varchar device_no "(nn)"
        decimal pcs_standby "(nn)"
        boolean has_battery "(nn)"
        varchar battery_maker
        varchar battery_model
        decimal battery_capacity
        int total_investment
        boolean has_subsidy "(nn)"
        varchar subsidy_name
        decimal subsidy_amount
        decimal pre_project_unit_price
    }

    en_r002_equipment_file {
        int id PK "auto-increment"
        boolean is_active "(nn) default true"
        timestamp created_at "(nn)"
        timestamp updated_at "(nn)"
        varchar created_by
        varchar updated_by
        int equipment_id FK "(nn) ON DELETE CASCADE"
        timestamp deleted_at "soft-delete"
        varchar deleted_by "soft-delete"
        varchar file_type "(nn)"
        varchar file_id "(nn)"
        text s3_key "(nn)"
    }

    participating_project {
        int id PK "auto-increment"
        boolean is_active "(nn) default true"
        timestamp created_at
        timestamp updated_at
        varchar created_by
        varchar updated_by
        int project_id FK "(nn) ON DELETE CASCADE"
        int user_id FK "(nn) references participating_user.id, ON DELETE CASCADE"
        boolean is_eligible "(nn)"
        int equipment_id FK "(nn) references en_r002_equipment.id, ON DELETE CASCADE"
        text s3_key_pdf_membership
    }

    %% <solar equipment: legacy registration flow (still live, coexists with en_r002_equipment)>
    site_r002_member_data {
        int id PK "auto-increment"
        boolean is_active "(nn) default true"
        timestamp created_at
        timestamp updated_at
        varchar created_by
        varchar updated_by
        int project_id FK "(nn) ON DELETE CASCADE"
        int site_id FK "(nn) ON DELETE CASCADE"
        int participating_company_id FK "(nn) ON DELETE CASCADE"
        varchar methodology_number "(nn) CHAR(3)"
        boolean is_eligible "(nn)"
        date equipment_op_date "(nn)"
        decimal nominal_max_output "(nn)"
        varchar pcs_manufacturer "(nn)"
        varchar pcs_model "(nn)"
        int power_cond_unit_count "(nn)"
        varchar device_no "(nn)"
        decimal power_cond_standby "(nn)"
        boolean has_battery "(nn)"
        varchar battery_maker
        varchar battery_model
        decimal battery_capacity
        decimal ict_equipment_power_consumption "(nn)"
        boolean additional_equipment
        date additional_equipment_date
        varchar equipment_type
        int unit_count
        varchar device_unique_no
        int total_investment
        boolean has_subsidy "(nn)"
        varchar subsidy_name
        decimal subsidy_amount
        decimal pre_project_unit_price
        timestamp deleted_at "soft-delete"
        varchar deleted_by "soft-delete"
    }

    %% <batch computation results>
    en_r002_reduction_result {
        int id PK "auto-increment"
        boolean is_active "(nn)"
        timestamp created_at
        varchar created_by
        timestamp updated_at
        varchar updated_by
        int batch_id "logical ref to batch_process_history.id, no DB FK"
        date processing_date
        int project_id FK "nullable, ON DELETE CASCADE"
        timestamp start_time
        timestamp end_time
        int site_id FK "nullable, ON DELETE CASCADE"
        int equipment_id FK "nullable, references en_r002_equipment.id, ON DELETE CASCADE"
        decimal empj
        decimal empj_m
        decimal empj_s
        decimal empj_s_control
        decimal empj_s_battery
        decimal elpj
        decimal embl
        decimal elbl
        decimal er
        timestamp deleted_at "soft-delete"
        varchar deleted_by "soft-delete"
    }

    en_r002_reduction_result_total {
        int id PK "auto-increment"
        boolean is_active "(nn)"
        timestamp created_at
        varchar created_by
        timestamp updated_at
        varchar updated_by
        int batch_id "UK; logical ref to batch_process_history.id, no DB FK"
        date processing_date
        int project_id FK "UK, nullable, ON DELETE CASCADE"
        timestamp start_time
        timestamp end_time
        decimal empj
        decimal empj_m
        decimal empj_s
        decimal empj_s_control
        decimal empj_s_battery
        decimal elpj
        decimal embl
        decimal elbl
        decimal er
        timestamp deleted_at "soft-delete"
        varchar deleted_by "soft-delete"
    }

    batch_process_history {
        int id PK "auto-increment"
        boolean is_active "(nn)"
        timestamp created_at
        varchar created_by
        timestamp updated_at
        varchar updated_by
        timestamp start_time
        timestamp end_time
        varchar status
        varchar claimed_by
        timestamp claimed_at
    }

    %% <cancel / audit>
    record_cancel_request {
        int id PK "auto-increment"
        boolean is_active "(nn)"
        timestamp created_at
        varchar created_by
        timestamp updated_at
        varchar updated_by
        varchar record_pk "(nn) DynamoDB PK"
        varchar record_sk "(nn) DynamoDB SK"
        varchar status
        varchar requested_by
        timestamp requested_at
        text reason
        varchar decided_by
        timestamp decided_at
        varchar offset_record_pk
        varchar offset_record_sk
        int site_id FK "(nn) ON DELETE CASCADE"
        timestamp deleted_at "soft-delete"
        varchar deleted_by "soft-delete"
    }

    %% <deletion / suspension — company lifecycle>
    company_deletion_request {
        int id PK "auto-increment"
        boolean is_active "(nn) default true"
        timestamp created_at
        timestamp updated_at
        varchar created_by
        varchar updated_by
        varchar requested_by "(nn)"
        timestamp requested_at "(nn)"
        text reason
        varchar status "(nn) default PENDING"
        varchar decided_by
        timestamp decided_at
        text rejection_reason
        varchar company_id FK "nullable, references participating_company.company_id (business key)"
    }

    company_suspension {
        int id PK "auto-increment"
        int company_id FK "(nn) references participating_company.id, ON DELETE CASCADE"
        varchar reason_code "(nn) POLICY_VIOLATION,SECURITY,ADMINISTRATIVE,OTHER"
        varchar reason_detail "(nn)"
        timestamp created_at "(nn)"
        varchar created_by "(nn) default 'system'"
        timestamp resolved_at "null = still active"
        varchar resolved_by
        varchar resolution_note
    }

    %% <security: password-reset / master-setup / signup-revision links>
    security_action_link {
        uuid id PK "app-generated"
        varchar purpose "(nn) MASTER_ACCOUNT_SETUP,REJECTED_SIGNUP_REVISION,PASSWORD_RESET"
        varchar subject_type "(nn) COMPANY,PARTICIPATING_USER,OPERATING_USER"
        int subject_id "(nn) polymorphic target row id, no DB FK"
        char token_hash UK "(nn) sha256 hex, 64 chars"
        varchar status "(nn) PENDING,OPENED,CONSUMED,REVOKED,EXPIRED"
        varchar delivery_status "(nn) PENDING,SENT,FAILED"
        int delivery_attempts "(nn) default 0"
        timestamp expires_at "(nn)"
        timestamp opened_at
        timestamp consumed_at
        timestamp revoked_at
        timestamp last_delivery_at
        timestamp created_at "(nn)"
        varchar created_by "(nn)"
    }

    %% <notifications>
    notification {
        int id PK "auto-increment"
        boolean is_active "(nn) default true"
        timestamp created_at
        varchar created_by
        timestamp updated_at
        varchar updated_by
        varchar title "(nn)"
        text message "(nn)"
        boolean read "(nn) default false"
        int operating_user_id FK "nullable; sender"
        int participating_user_id FK "nullable; recipient"
    }

    %% <relationships>
    operator_company ||--o{ operating_user : "employs"
    operator_company ||--o{ account : "manages"
    operator_company ||--o{ project : "owns"

    participating_company ||--o{ site : "has"
    participating_company ||--o{ participating_user : "employs"
    participating_company ||--o{ site_r002_member_data : "registers equipment (legacy)"
    participating_company ||--o{ company_suspension : "suspension history"
    participating_company |o--o{ company_deletion_request : "deletion requests (business-key FK)"

    site |o--o{ participating_user : "assigned to (nullable)"
    site ||--o{ site_project_relation : "linked from"
    site ||--o{ record_cancel_request : "cancel requests"
    site ||--o{ en_r002_equipment : "equipment"
    site ||--o{ site_r002_member_data : "site_id FK (legacy)"
    site |o--o{ en_r002_reduction_result : "site_id FK (nullable)"

    project ||--o{ site_project_relation : "linked from"
    project |o--|| project_files : "has"
    project ||--o{ site_r002_member_data : "project_id FK (legacy)"
    project ||--o{ participating_project : "links"
    project |o--o{ en_r002_reduction_result : "project_id FK (nullable)"
    project |o--|| en_r002_reduction_result_total : "project_id FK (unique)"

    en_r002_equipment ||--o{ en_r002_equipment_file : "evidence files"
    en_r002_equipment ||--o{ participating_project : "equipment_id FK"
    en_r002_equipment |o--o{ en_r002_reduction_result : "equipment_id FK (nullable)"

    participating_user ||--o{ participating_project : "user_id FK"

    notification }o--o| operating_user : "sender (opt)"
    notification }o--o| participating_user : "recipient (opt)"
```

---

補足:

- 本図は `db/GXCarbonSQL_current_2026-07-28.sql`（`... /entity/` 配下の全 `@Entity` に完全準拠したベースライン）を正として作成。差分が出た場合はそちらの SQL を正とする。
- **設備登録フローが2系統ある**: `en_r002_equipment`（`participating_project` 経由の現行フロー）と `site_r002_member_data`（`site`/`project`/`participating_company` に直接 FK を持つ旧フロー）が両方とも現役で共存している。片方だけ見て設計判断しないこと。
- `security_action_link` はパスワードリセット・マスターアカウント初期設定・却下サインアップの再修正リンクを扱う汎用テーブル。`subject_type` + `subject_id` による多態参照で、DB 上の FK 制約は張られていない（アプリ側で整合性を保証）。
- `en_r002_reduction_result` / `en_r002_reduction_result_total` の `batch_id` は `batch_process_history.id` への論理参照のみで DB FK は無い。一方 `project_id` / `site_id` / `equipment_id` は実 FK（旧図では project/site も論理参照扱いだったが現在は実 FK）。
- `operating_user.company_id` / `participating_user.company_id` はどちらも会社テーブルの surrogate `id`（int）を参照する実 FK。ビジネスキーの `company_id`（varchar）ではない点に注意（旧図の記載は誤り）。
- `company_deletion_request.company_id` のみ例外的に `participating_company.company_id`（ビジネスキー, varchar）を参照する。
- `participating_company` は `company_status`（PENDING → APPROVED_PENDING_SETUP → ACTIVE / SUSPENDED / REJECTED）で状態管理され、`is_active` は `company_status = 'ACTIVE'` と常に一致する CHECK 制約がある。停止理由の履歴は `company_suspension` に記録される。
- 住所モデルが `city` + `town` + `address` から `municipality` + `blocknum` + `building_name`(nullable) に変更されている（`operator_company` / `participating_company` / `site`。ただし `account` の `holder_*` 系は旧来の `holder_city` / `holder_address` のまま）。
- `project` と `site` に公開用の `public_id`（UNIQUE, 32 chars）が追加されている。
- `operating_user` / `participating_user` に `email_otp_login_enabled` と `auth_version`（トークン失効管理用）が追加されている。
- `site_project_relation` から旧図にあった `application_date` と `participating_company_id` 列は削除され、`activity_start_date` + `status` の構成になった。`(project_id, site_id)` の UNIQUE 制約は維持。
- 旧図に載っていたが現在は不使用/孤立テーブルのため削除: `subscription_log`, `batch_processing_history`（`batch_process_history` とは別物）, `site_r002_equipment_file`（`en_r002_equipment_file` に統合）。
- DynamoDB 上のエンティティ（`ConfirmationItems`, `EnR002ApplicationConfirmation`, `EnR002MonitoringRecord`, `EnR002SustainabilityCommitment`, `ProjectDocumentStatus`, `ProjectParticipationConfirmation` など）は `@DynamoDbBean` であり、この関係図（RDB スキーマ）には含めていない。

---
