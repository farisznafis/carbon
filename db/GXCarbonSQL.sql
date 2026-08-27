-- ============================================================================
-- GXCarbon schema — 2026-08-07
-- Complete, code-accurate baseline: matches every @Entity in
-- src/main/java/com/kct/jcredit/core_backend/entity/ as of this date.
-- Intended for setting up a fresh database from scratch (DROP + CREATE),
-- same spirit as the original db/GXCarbonSQL.sql.
--
-- Supersedes: db/GXCarbonSQL.sql (outdated — no public_id, no company_status,
-- no company_suspension/security_action_link, wrong company_deletion_request
-- type, missing participating_project's current shape) and
-- db/GXCarbonSQL_current_2026-07-26.sql (missing participating_project and
-- the two constraints below).
--
-- Includes everything from:
--   - migration_2026-07-26_fixes.sql
--   - migration_2026-07-26_optional-constraints.sql (safe here since this is
--     a fresh CREATE — no existing data to violate batch_id/project_id
--     uniqueness or file_type/file_id NOT NULL)
--   - migration_2026-07-27_create-participating-project.sql
--   - monitoring_data_deletion_ddl.sql (monitoring_data_deletion_history,
--     project.status_finalized_at)
--
-- operating_user.role: correct set is ('admin','operator'); the live DB's
-- operating_user_role_check currently allows ('operator','user') instead,
-- which is out of date. Needs a manual ALTER on the live DB to match.
--
-- site_r002_member_data: intentionally DROPped and NOT recreated.
-- SiteR002MemberData / its service is deprecated and being removed from the
-- codebase; once that lands, drop the DROP TABLE line too.
--
-- Intentionally NOT included (dropped/orphaned, not used by any entity):
--   subscription_log, batch_processing_history, site_r002_equipment_file,
--   site_r002_member_data (deprecated, pending removal — see note above)
-- ============================================================================

-- ============================
-- DROP TABLES (dependency-safe)
-- ============================

DROP TABLE IF EXISTS monitoring_data_deletion_history CASCADE;
DROP TABLE IF EXISTS notification CASCADE;
DROP TABLE IF EXISTS en_r002_reduction_result_total CASCADE;
DROP TABLE IF EXISTS en_r002_reduction_result CASCADE;
DROP TABLE IF EXISTS en_r002_equipment_file CASCADE;
DROP TABLE IF EXISTS site_r002_member_data CASCADE;
DROP TABLE IF EXISTS site_project_relation CASCADE;
DROP TABLE IF EXISTS record_cancel_request CASCADE;
DROP TABLE IF EXISTS participating_project CASCADE;
DROP TABLE IF EXISTS participating_user CASCADE;
DROP TABLE IF EXISTS en_r002_equipment CASCADE;
DROP TABLE IF EXISTS project_files CASCADE;
DROP TABLE IF EXISTS company_suspension CASCADE;
DROP TABLE IF EXISTS company_deletion_request CASCADE;
DROP TABLE IF EXISTS site CASCADE;
DROP TABLE IF EXISTS operating_user CASCADE;
DROP TABLE IF EXISTS project CASCADE;
DROP TABLE IF EXISTS account CASCADE;
DROP TABLE IF EXISTS security_action_link CASCADE;
DROP TABLE IF EXISTS batch_process_history CASCADE;
DROP TABLE IF EXISTS participating_company CASCADE;
DROP TABLE IF EXISTS operator_company CASCADE;
DROP TABLE IF EXISTS permission CASCADE;
DROP TABLE IF EXISTS operating_user_permission CASCADE;


-- ============================
-- CREATE TABLES
-- ============================

-- Operator Company
CREATE TABLE operator_company (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    is_active BOOLEAN NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    company_id VARCHAR(12) NOT NULL UNIQUE,
    name_ja VARCHAR(255) NOT NULL,
    name_furigana VARCHAR(255) NOT NULL,
    name_en VARCHAR(255) NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    corp_number VARCHAR(13) NOT NULL UNIQUE CHECK (corp_number ~ '^[0-9]{13}$'),
    postal_code VARCHAR(8) NOT NULL CHECK (postal_code ~ '^[0-9]{3}-?[0-9]{4}$'),
    prefecture VARCHAR(10) NOT NULL,
    municipality VARCHAR(150) NOT NULL,
    blocknum VARCHAR(100) NOT NULL,
    building_name VARCHAR(255),
    rep_first_name VARCHAR(50) NOT NULL,
    rep_first_name_kana VARCHAR(50) NOT NULL,
    rep_last_name VARCHAR(50) NOT NULL,
    rep_last_name_kana VARCHAR(50) NOT NULL,
    rep_email VARCHAR(254) NOT NULL,
    rep_phone_number VARCHAR(20) NOT NULL,
    rep_position VARCHAR(100) NOT NULL
);

-- Participating Company
CREATE TABLE participating_company (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    company_id VARCHAR(12) NOT NULL UNIQUE,
    name_ja VARCHAR(255) NOT NULL,
    name_furigana VARCHAR(255) NOT NULL,
    name_en VARCHAR(255) NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    corp_number VARCHAR(13) NOT NULL UNIQUE CHECK (corp_number ~ '^[0-9]{13}$'),
    postal_code VARCHAR(8) NOT NULL CHECK (postal_code ~ '^[0-9]{3}-?[0-9]{4}$'),
    prefecture VARCHAR(10) NOT NULL,
    municipality VARCHAR(150) NOT NULL,
    blocknum VARCHAR(100) NOT NULL,
    building_name VARCHAR(255),
    fax VARCHAR(20),
    rep_first_name VARCHAR(50) NOT NULL,
    rep_first_name_kana VARCHAR(50) NOT NULL,
    rep_last_name VARCHAR(50) NOT NULL,
    rep_last_name_kana VARCHAR(50) NOT NULL,
    rep_phone_number VARCHAR(20) NOT NULL,
    rep_position VARCHAR(100) NOT NULL,
    requested_methodologies TEXT,
    activity_information TEXT,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100),
    company_status VARCHAR(30) NOT NULL DEFAULT 'PENDING'
        CHECK (company_status IN ('PENDING','APPROVED_PENDING_SETUP','ACTIVE','SUSPENDED','REJECTED')),
    email_verified_at TIMESTAMP,
    last_rejection_reason VARCHAR(1000),
    rejected_at TIMESTAMP,
    CONSTRAINT chk_participating_company_active_status
        CHECK (is_active = (company_status = 'ACTIVE'))
);

-- Batch Process History
CREATE TABLE batch_process_history (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    is_active BOOLEAN NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    status VARCHAR(10),
    claimed_by VARCHAR(50),
    claimed_at TIMESTAMP
);

-- Security Action Link (master-setup / password-reset / signup-revision links)
CREATE TABLE security_action_link (
    id UUID PRIMARY KEY,
    purpose VARCHAR(40) NOT NULL
        CHECK (purpose IN ('MASTER_ACCOUNT_SETUP','REJECTED_SIGNUP_REVISION','PASSWORD_RESET')),
    subject_type VARCHAR(30) NOT NULL
        CHECK (subject_type IN ('COMPANY','PARTICIPATING_USER','OPERATING_USER')),
    subject_id INT NOT NULL,
    token_hash CHAR(64) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL
        CHECK (status IN ('PENDING','OPENED','CONSUMED','REVOKED','EXPIRED')),
    delivery_status VARCHAR(20) NOT NULL
        CHECK (delivery_status IN ('PENDING','SENT','FAILED')),
    delivery_attempts INT NOT NULL DEFAULT 0,
    expires_at TIMESTAMP NOT NULL,
    opened_at TIMESTAMP,
    consumed_at TIMESTAMP,
    revoked_at TIMESTAMP,
    last_delivery_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL
);

-- Account
CREATE TABLE account (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    is_active BOOLEAN NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    acct_no VARCHAR(20) NOT NULL UNIQUE,
    holder_account_name_kana VARCHAR(50) NOT NULL,
    holder_account_name VARCHAR(50) NOT NULL,
    holder_corp_number VARCHAR(13) UNIQUE CHECK (holder_corp_number ~ '^[0-9]{13}$'),
    holder_prefecture VARCHAR(100) NOT NULL,
    holder_city VARCHAR(100) NOT NULL,
    holder_address VARCHAR(500) NOT NULL,
    is_holder_undecided BOOLEAN NOT NULL,
    operator_company_id INT NOT NULL REFERENCES operator_company(id)
);

-- Project
CREATE TABLE project (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    is_active BOOLEAN NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    project_name VARCHAR(100) NOT NULL,
    project_number VARCHAR(10) NOT NULL UNIQUE CHECK (project_number ~ '^P[0-9]+$'),
    project_status VARCHAR(10) NOT NULL,
    rep_first_name VARCHAR(50) NOT NULL,
    rep_first_name_kana VARCHAR(50) NOT NULL,
    rep_last_name VARCHAR(50) NOT NULL,
    rep_last_name_kana VARCHAR(50) NOT NULL,
    rep_email VARCHAR(254) NOT NULL,
    rep_phone_number VARCHAR(20) NOT NULL,
    rep_position VARCHAR(100) NOT NULL,
    pic_first_name VARCHAR(50) NOT NULL,
    pic_first_name_kana VARCHAR(50) NOT NULL,
    pic_last_name VARCHAR(50) NOT NULL,
    pic_last_name_kana VARCHAR(50) NOT NULL,
    pic_email VARCHAR(254) NOT NULL,
    pic_phone_number VARCHAR(20) NOT NULL,
    pic_position VARCHAR(100) NOT NULL,
    data_management_pic VARCHAR(50) NOT NULL,
    emission_reduction DECIMAL(12,1),
    baseline_emission DECIMAL(12,1),
    post_project_emission DECIMAL(12,1),
    regional_category VARCHAR(10) NOT NULL
        CHECK (regional_category IN ('北海道','東北','関東','中部','近畿','中国','四国','九州・沖縄','全国')),
    new_members_total INT NOT NULL,
    withdrawn_members INT NOT NULL,
    cumulative_members_total INT NOT NULL,
    members_total INT,
    methodology_version VARCHAR(10) NOT NULL CHECK (methodology_version ~ '^Ver\.[0-9]+(\.[0-9]+)?$'),
    method_category VARCHAR(10) NOT NULL CHECK (method_category IN ('EN-S','EN-R','IN','AG','WA')),
    method_number VARCHAR(3) NOT NULL UNIQUE CHECK (method_number ~ '^[0-9]{3}$'),
    method_name VARCHAR(255) NOT NULL,
    method_main_emission VARCHAR(255) NOT NULL,
    method_combi_feasibility BOOLEAN NOT NULL,
    method_details VARCHAR(1000) NOT NULL,
    operator_company_id INT NOT NULL REFERENCES operator_company(id),
    public_id VARCHAR(32) NOT NULL UNIQUE,
    status_finalized_at TIMESTAMP
);

-- Operating User
CREATE TABLE operating_user (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    is_active BOOLEAN NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    username VARCHAR(50) NOT NULL UNIQUE,
    first_name VARCHAR(50) NOT NULL,
    first_name_kana VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    last_name_kana VARCHAR(50) NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(10) NOT NULL CHECK (role IN ('admin', 'operator')),
    login_attempts INT NOT NULL,
    account_locked_until TIMESTAMP,
    company_id INT NOT NULL REFERENCES operator_company(id),
    email_otp_login_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    auth_version INT NOT NULL DEFAULT 0
);

CREATE TABLE permission (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    code VARCHAR(80) NOT NULL UNIQUE,

    display_name VARCHAR(100) NOT NULL,

    category VARCHAR(50) NOT NULL,

    is_active BOOLEAN NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE TABLE operating_user_permission (
    operating_user_id INT NOT NULL
        REFERENCES operating_user(id)
        ON DELETE CASCADE,

    permission_id INT NOT NULL
        REFERENCES permission(id)
        ON DELETE RESTRICT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),

    PRIMARY KEY (operating_user_id, permission_id)
);

INSERT INTO permission
    (code, display_name, category, is_active)
VALUES
    ('PROJECT_CREATE', 'プロジェクト作成', 'PROJECT', TRUE),
    ('PROJECT_READ', 'プロジェクト参照', 'PROJECT', TRUE),
    ('PROJECT_UPDATE', 'プロジェクト更新', 'PROJECT', TRUE),
    ('PROJECT_STATUS_UPDATE', 'プロジェクトステータス変更', 'PROJECT', TRUE),
    ('PROJECT_APPLICATION_OUTPUT', '申請書出力', 'PROJECT', TRUE),

    ('PARTICIPATING_COMPANY_READ', '参加企業参照', 'PARTICIPATING_COMPANY', TRUE),
    ('PARTICIPATING_COMPANY_APPLICATION_REVIEW', '参加申請処理', 'PARTICIPATING_COMPANY', TRUE),
    ('PARTICIPATING_COMPANY_SUSPEND', '参加企業一時停止', 'PARTICIPATING_COMPANY', TRUE),
    ('MASTER_USER_PASSWORD_RESET', 'マスターユーザーパスワードリセット', 'PARTICIPATING_COMPANY', TRUE),

    ('MONITORING_READ', 'モニタリングデータ参照', 'MONITORING', TRUE),
    ('MONITORING_DELETE', 'モニタリングデータ削除', 'MONITORING', TRUE),
    ('MONITORING_CANCEL', 'モニタリングデータ取消', 'MONITORING', TRUE),
    ('MONITORING_BATCH_HISTORY_READ', 'バッチ履歴参照', 'MONITORING', TRUE),
    ('MONITORING_BATCH_ERROR_READ', 'バッチエラー参照', 'MONITORING', TRUE),

    ('OPERATOR_USER_READ', '運用者一覧参照', 'USER_MANAGEMENT', TRUE),
    ('OPERATOR_USER_CREATE', '運用者作成', 'USER_MANAGEMENT', TRUE),
    ('OPERATOR_USER_UPDATE', '運用者更新', 'USER_MANAGEMENT', TRUE),

    ('METHODOLOGY_READ', '方法論参照', 'METHODOLOGY', TRUE),
    ('APPLICATION_FORM_READ', '申請書様式参照', 'APPLICATION_FORM', TRUE);

-- Site
CREATE TABLE site (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    is_active BOOLEAN NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    site_name VARCHAR(255) NOT NULL,
    postal_code VARCHAR(8) NOT NULL CHECK (postal_code ~ '^[0-9]{3}-?[0-9]{4}$'),
    prefecture VARCHAR(10) NOT NULL,
    municipality VARCHAR(150) NOT NULL,
    blocknum VARCHAR(100) NOT NULL,
    building_name VARCHAR(255),
    pic_first_name VARCHAR(50) NOT NULL,
    pic_first_name_kana VARCHAR(50) NOT NULL,
    pic_last_name VARCHAR(50) NOT NULL,
    pic_last_name_kana VARCHAR(50) NOT NULL,
    pic_email VARCHAR(254) NOT NULL,
    pic_phone_number VARCHAR(20) NOT NULL,
    pic_position VARCHAR(100) NOT NULL,
    participating_company_id INT NOT NULL REFERENCES participating_company(id) ON DELETE CASCADE,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100),
    public_id VARCHAR(32) NOT NULL UNIQUE
);

-- Company Deletion Request
CREATE TABLE company_deletion_request (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    requested_by VARCHAR(100) NOT NULL,
    requested_at TIMESTAMP NOT NULL,
    reason TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    decided_by VARCHAR(100),
    decided_at TIMESTAMP,
    rejection_reason TEXT,
    company_id VARCHAR(12) REFERENCES participating_company(company_id)
);

-- Company Suspension
CREATE TABLE company_suspension (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_id INT NOT NULL REFERENCES participating_company(id) ON DELETE CASCADE,
    reason_code VARCHAR(30) NOT NULL
        CHECK (reason_code IN ('POLICY_VIOLATION','SECURITY','ADMINISTRATIVE','OTHER')),
    reason_detail VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    resolved_at TIMESTAMP,
    resolved_by VARCHAR(100),
    resolution_note VARCHAR(500)
);

-- Project Files
CREATE TABLE project_files (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    project_id INT NOT NULL UNIQUE REFERENCES project(id) ON UPDATE CASCADE ON DELETE CASCADE,
    certificate_registration TEXT,
    report_validation TEXT,
    report_verification TEXT
);

-- en_r002_equipment
CREATE TABLE en_r002_equipment (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100),
    site_id INT NOT NULL REFERENCES site(id) ON DELETE CASCADE,
    change_date DATE,
    previous_equipment_id INT,
    equipment_op_date DATE NOT NULL,
    nominal_max_output DECIMAL(10,1) NOT NULL,
    pcs_manufacturer VARCHAR(100) NOT NULL,
    pcs_model VARCHAR(50) NOT NULL,
    pcs_unit_count INT,
    device_no VARCHAR(100) NOT NULL,
    pcs_standby DECIMAL(8,2) NOT NULL,
    has_battery BOOLEAN NOT NULL,
    battery_maker VARCHAR(100),
    battery_model VARCHAR(100),
    battery_capacity DECIMAL(10,1),
    total_investment INT,
    has_subsidy BOOLEAN NOT NULL,
    subsidy_name VARCHAR(200),
    subsidy_amount DECIMAL(15,0),
    pre_project_unit_price DECIMAL(10,2),
    CONSTRAINT fk_eq_previous FOREIGN KEY (previous_equipment_id)
        REFERENCES en_r002_equipment(id)
);

-- Participating User
CREATE TABLE participating_user (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    is_active BOOLEAN NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    username VARCHAR(50) NOT NULL UNIQUE,
    first_name VARCHAR(50) NOT NULL,
    first_name_kana VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    last_name_kana VARCHAR(50) NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    user_position VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('master', 'user')),
    login_attempts INT NOT NULL,
    account_locked_until TIMESTAMP,
    site_id INT REFERENCES site(id) ON DELETE SET NULL,
    company_id INT NOT NULL REFERENCES participating_company(id),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100),
    email_otp_login_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    auth_version INT NOT NULL DEFAULT 0
);

-- participating_project
-- Links a project + participating_company + equipment together. Was missing
-- from the live DB entirely until migration_2026-07-27_create-participating-project.sql.
CREATE TABLE participating_project (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    status VARCHAR(20) NOT NULL
        CHECK (status IN ('APPROVED','OPEN','REJECTED')),
    project_id INT NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    company_id INT NOT NULL REFERENCES participating_company(id) ON DELETE CASCADE,
    is_eligible BOOLEAN NOT NULL,
    equipment_id INT NOT NULL REFERENCES en_r002_equipment(id) ON DELETE CASCADE,
    s3_key_pdf_membership TEXT,
    docusign_envelope_id VARCHAR(255),
    signing_status VARCHAR(50)
);

-- Record Cancel Request
CREATE TABLE record_cancel_request (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    is_active BOOLEAN NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    record_pk VARCHAR(10) NOT NULL,
    record_sk VARCHAR(50) NOT NULL,
    status VARCHAR(50),
    requested_by VARCHAR(100),
    requested_at TIMESTAMP,
    reason TEXT,
    decided_by VARCHAR(100),
    decided_at TIMESTAMP,
    offset_record_pk VARCHAR(10),
    offset_record_sk VARCHAR(50),
    site_id INT NOT NULL REFERENCES site(id) ON UPDATE CASCADE ON DELETE CASCADE,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100)
);

-- site_project_relation
CREATE TABLE site_project_relation (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    is_active BOOLEAN NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    project_id INT NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    site_id INT NOT NULL REFERENCES site(id) ON DELETE CASCADE,
    activity_start_date DATE NOT NULL,
    status VARCHAR(100) NOT NULL,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100),
    CONSTRAINT uq_site_project_relation_project_site UNIQUE (project_id, site_id)
);

-- en_r002_equipment_file
CREATE TABLE en_r002_equipment_file (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    equipment_id INT NOT NULL REFERENCES en_r002_equipment(id) ON DELETE CASCADE,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100),
    file_type VARCHAR(50) NOT NULL,
    file_id VARCHAR(100) NOT NULL,
    s3_key TEXT NOT NULL
);

CREATE TABLE en_r002_reduction_result (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    is_active BOOLEAN NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    batch_id INT,
    processing_date DATE,
    project_id INT REFERENCES project(id) ON DELETE CASCADE,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    site_id INT REFERENCES site(id) ON DELETE CASCADE,
    equipment_id INT REFERENCES en_r002_equipment(id) ON DELETE CASCADE,
    empj DECIMAL(12,3),
    empj_m DECIMAL(12,3),
    empj_s DECIMAL(12,3),
    empj_s_control DECIMAL(12,3),
    empj_s_battery DECIMAL(12,3),
    elpj DECIMAL(12,3),
    embl DECIMAL(12,3),
    elbl DECIMAL(12,3),
    er DECIMAL(12,3),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100)
);

CREATE TABLE en_r002_reduction_result_total (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    is_active BOOLEAN NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    batch_id INT,
    processing_date DATE,
    project_id INT REFERENCES project(id) ON DELETE CASCADE,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    empj DECIMAL(12,3),
    empj_m DECIMAL(12,3),
    empj_s DECIMAL(12,3),
    empj_s_control DECIMAL(12,3),
    empj_s_battery DECIMAL(12,3),
    elpj DECIMAL(12,3),
    embl DECIMAL(12,3),
    elbl DECIMAL(12,3),
    er DECIMAL(12,3),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100),
    CONSTRAINT uq_en_r002_reduction_result_total_batch_id UNIQUE (batch_id),
    CONSTRAINT uq_en_r002_reduction_result_total_project_id UNIQUE (project_id)
);

-- Notification
CREATE TABLE notification (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    title VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    read BOOLEAN NOT NULL DEFAULT FALSE,
    operating_user_id INT REFERENCES operating_user(id) ON UPDATE CASCADE ON DELETE CASCADE,
    participating_user_id INT REFERENCES participating_user(id) ON UPDATE CASCADE ON DELETE CASCADE
);

-- Monitoring Data Deletion History
CREATE TABLE monitoring_data_deletion_history (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    monitoring_record_id VARCHAR(100) NOT NULL,
    record_time_updated VARCHAR(50) NOT NULL,
    date_recorded VARCHAR(10),
    project_id INT NOT NULL,
    site_id INT,
    result VARCHAR(10) NOT NULL,
    failure_reason VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);


-- ============================
-- INDEXES
-- ============================

CREATE INDEX idx_company_suspension_company_created
    ON company_suspension (company_id, created_at DESC, id DESC);

CREATE INDEX idx_participating_company_status_created
    ON participating_company (company_status, created_at DESC, id DESC)
    WHERE (deleted_at IS NULL);

CREATE INDEX idx_monitoring_deletion_history_project_id
    ON monitoring_data_deletion_history (project_id);

CREATE INDEX idx_participating_user_company_id ON participating_user (company_id);

CREATE INDEX idx_participating_user_site_id ON participating_user (site_id);

CREATE INDEX idx_security_action_expiry ON security_action_link (expires_at, status);

CREATE INDEX idx_security_action_subject
    ON security_action_link (subject_type, subject_id, purpose, status);

CREATE INDEX idx_site_participating_company_id ON site (participating_company_id);

CREATE UNIQUE INDEX uq_company_suspension_active_reason
    ON company_suspension (company_id, reason_code)
    WHERE (resolved_at IS NULL);

CREATE UNIQUE INDEX uq_operating_user_email_ci
    ON operating_user (lower(btrim(email)));

CREATE UNIQUE INDEX uq_operating_user_username_ci
    ON operating_user (lower(btrim(username)));

CREATE UNIQUE INDEX uq_participating_user_email_ci
    ON participating_user (lower(btrim(email)))
    WHERE (deleted_at IS NULL);

CREATE UNIQUE INDEX uq_participating_user_username_ci
    ON participating_user (lower(btrim(username)))
    WHERE (deleted_at IS NULL);

CREATE UNIQUE INDEX uq_security_action_active
    ON security_action_link (subject_type, subject_id, purpose)
    WHERE (status IN ('PENDING', 'OPENED'));
